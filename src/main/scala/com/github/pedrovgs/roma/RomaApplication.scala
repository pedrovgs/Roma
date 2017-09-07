package com.github.pedrovgs.roma

import com.github.pedrovgs.roma.Console._
import com.github.pedrovgs.roma.config.{ConfigLoader, FirebaseConfig, MachineLearningConfig, TwitterConfig}
import com.github.pedrovgs.roma.machinelearning.{FeaturesExtractor, TweetsClassifier}
import com.github.pedrovgs.roma.storage.{StatsStorage, TweetsStorage}
import org.apache.spark.mllib.classification.SVMModel
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.twitter.TwitterUtils
import twitter4j.Status
import twitter4j.auth.{Authorization, OAuthAuthorization}
import twitter4j.conf.ConfigurationBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object RomaApplication extends SparkApp with Resources {

  val appName: String = "Roma"

  override def main(args: Array[String]): Unit = {
    super.main(args)
    print("Initializing...")
    separator()
    val twitterCredentials = loadTwitterCredentials()
    smallSeparator()
    val firebaseCredentials = loadFirebaseCredentials()
    smallSeparator()
    val machineLearningConfig = loadMachineLearningConfig()
    smallSeparator()
    (firebaseCredentials, twitterCredentials, machineLearningConfig) match {
      case (Some(_), Some(twitterConfig), Some(mlConfig)) =>
        val authorization = authorizeTwitterStream(twitterConfig)
        startStreaming(authorization, mlConfig)
      case _ => print("Finishing application")
    }
  }

  private def authorizeTwitterStream(twitterConfig: TwitterConfig) = {
    val configuration = new ConfigurationBuilder()
      .setOAuthConsumerKey(twitterConfig.consumerKey)
      .setOAuthConsumerSecret(twitterConfig.consumerSecret)
      .setOAuthAccessToken(twitterConfig.accessToken)
      .setOAuthAccessTokenSecret(twitterConfig.accessTokenSecret)
      .build()
    val authorization = new OAuthAuthorization(configuration)
    authorization
  }

  private def twitterStream(authorization: Authorization) =
    TwitterUtils.createFilteredStream(streamingContext, Some(authorization))

  def loadFirebaseCredentials(): Option[FirebaseConfig] = {
    print("Loading Firebase configuration")
    ConfigLoader.loadFirebaseConfig() match {
      case Some(firebaseConfig) =>
        print("Firebase configuration loaded: " + firebaseConfig)
        Some(firebaseConfig)
      case None =>
        print("Firebase configuration couldn't be loaded. Review your resources/application.conf file")
        None
    }
  }

  private def loadTwitterCredentials(): Option[TwitterConfig] = {
    print("Loading Twitter configuration")
    ConfigLoader.loadTwitterConfig() match {
      case Some(twitterConfig) =>
        print("Twitter configuration loaded: " + twitterConfig)
        Some(twitterConfig)
      case None =>
        print("Twitter configuration couldn't be loaded. Review your resources/application.conf file")
        None
    }
  }

  private def loadMachineLearningConfig(): Option[MachineLearningConfig] = {
    print("Loading Twitter configuration")
    ConfigLoader.loadMachineLearningTrainingConfig() match {
      case Some(machineLearningConfig) =>
        print("Machine learning configuration loaded: " + machineLearningConfig)
        Some(machineLearningConfig)
      case None =>
        print("Twitter configuration couldn't be loaded. Review your resources/application.conf file")
        None
    }
  }

  private def startStreaming(authorization: Authorization, machineLearningConfig: MachineLearningConfig) = {
    print("Let's start reading tweets!")
    separator()
    val modelPath = getModelPath(machineLearningConfig)
    val svmModel  = SVMModel.load(sparkContext, modelPath)
    twitterStream(authorization)
      .filter(_.getLang == "en")
      .filter(!_.isRetweet)
      .filter(!_.getText.startsWith("RT"))
      .foreachRDD { rdd: RDD[Status] =>
        if (!rdd.isEmpty()) {
          val classifiedTweets: RDD[ClassifiedTweet] = classifyTweets(rdd, svmModel, machineLearningConfig)
          saveTweets(classifiedTweets.filter(_.sentiment != Sentiment.Neutral.toString))
          updateStats(classifiedTweets)
          smallSeparator()
        }
      }
    streamingContext.start()
    streamingContext.awaitTermination()
    print("Application finished")
  }

  private def getModelPath(machineLearningConfig: MachineLearningConfig) = {
    val modelPath = getFilePath("/" + machineLearningConfig.modelFileName)
    if (modelPath.isEmpty && args.length > 0) {
      args(0)
    } else {
      modelPath
    }
  }

  private def classifyTweets(status: RDD[Status],
                             svmModel: SVMModel,
                             machineLearningConfig: MachineLearningConfig): RDD[ClassifiedTweet] = {
    import sqlContext.implicits._
    print("Let's analyze a bunch of tweets!")
    val tweets = status
      .map { status =>
        status.getText
      }
      .toDF(TweetColumns.tweetContentColumnName)
    val featurizedTweets = FeaturesExtractor.extract(tweets)
    val classifiedTweets = TweetsClassifier.classify(sqlContext, svmModel, featurizedTweets)
    classifiedTweets.rdd
      .map { row =>
        val content    = row.getAs[String](TweetColumns.tweetContentColumnName)
        val classScore = row.getAs[Double](TweetColumns.classificationColumnName)
        val sentiment =
          if (classScore <= machineLearningConfig.positiveThreshold && classScore >= machineLearningConfig.negativeThreshold)
            Sentiment.Neutral
          else if (classScore > machineLearningConfig.positiveThreshold) Sentiment.Positive
          else Sentiment.Negative
        ClassifiedTweet(content, sentiment.toString, classScore)
      }
  }

  private def saveTweets(classifiedTweets: RDD[ClassifiedTweet]): Unit = {
    TweetsStorage.clear()
    classifiedTweets.foreachPartition { classifiedTweetsPerPartition =>
      TweetsStorage
        .saveTweets(classifiedTweetsPerPartition.toSeq)
        .onComplete {
          case Success(savedTweets) =>
            print("Tweets saved properly!")
            savedTweets.foreach(print(_))
          case Failure(_) => print("Error saving tweets :_(")
        }
    }

  }

  private def updateStats(classifiedTweets: RDD[ClassifiedTweet]): Unit = {
    val tweetsStats: ClassificationStats = calculateClassificationStats(classifiedTweets)
    if (tweetsStats.numberOfClassifiedTweets > 0) {
      StatsStorage
        .updateStats(tweetsStats)
        .onComplete {
          case Success(Some(stats)) =>
            print("Classified tweet stats updated properly!")
            print(stats)
          case _ => print("Error updating stats :_(")
        }
    }
  }

  private def calculateClassificationStats(classifiedTweets: RDD[ClassifiedTweet]) = {
    val numberOfTweets = classifiedTweets.count()
    val positiveTweets = sparkContext.longAccumulator
    val negativeTweets = sparkContext.longAccumulator
    val neutralTweets  = sparkContext.longAccumulator
    classifiedTweets.foreach { tweet =>
      if (tweet.sentiment == Sentiment.Positive.toString) {
        positiveTweets.add(1)
      } else if (tweet.sentiment == Sentiment.Negative.toString) {
        negativeTweets.add(1)
      } else {
        neutralTweets.add(1)
      }
    }
    val stats = ClassificationStats(numberOfTweets, positiveTweets.value, negativeTweets.value, neutralTweets.value)
    positiveTweets.reset()
    negativeTweets.reset()
    neutralTweets.reset()
    stats
  }

  private def clearData(): Unit = {
    TweetsStorage.clear()
    StatsStorage.clear()
  }
}
