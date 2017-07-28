package com.github.pedrovgs.roma

import com.github.pedrovgs.roma.config.{ConfigLoader, FirebaseConfig, TwitterConfig}
import com.github.pedrovgs.roma.storage.{Firebase, TweetsStorage}
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.twitter.TwitterUtils
import twitter4j.Status
import twitter4j.auth.{Authorization, OAuthAuthorization}
import twitter4j.conf.ConfigurationBuilder
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object RomaApplication extends SparkApp {

  val appName: String = "Roma"

  private lazy val storage = sparkContext.broadcast(new TweetsStorage(new Firebase))

  private def twitterStream(authorization: Authorization) =
    TwitterUtils.createFilteredStream(streamingContext, Some(authorization))

  def loadFirebaseCredentials(): Option[FirebaseConfig] = {
    pprint.pprintln("Loading Firebase configuration")
    ConfigLoader.loadFirebaseConfig() match {
      case Some(firebaseConfig) =>
        pprint.pprintln("Firebase configuration loaded: " + firebaseConfig)
        Some(firebaseConfig)
      case None =>
        pprint.pprintln("Firebase configuration couldn't be loaded. Review your resources/application.conf file")
        None
    }
  }

  private def loadTwitterCredentials(): Option[TwitterConfig] = {
    pprint.pprintln("Loading Twitter configuration")
    ConfigLoader.loadTwitterConfig() match {
      case Some(twitterConfig) =>
        pprint.pprintln("Twitter configuration loaded: " + twitterConfig)
        Some(twitterConfig)
      case None =>
        pprint.pprintln("Twitter configuration couldn't be loaded. Review your resources/application.conf file")
        None
    }
  }

  private def startStreaming(authorization: Authorization) = {
    pprint.pprintln("Let's start reading tweets!")
    twitterStream(authorization)
      .filter(_.getLang == "en")
      .foreachRDD { rdd: RDD[Status] =>
        if (!rdd.isEmpty()) {
          pprint.pprintln("Let's analyze a bunch of tweets!")
          val classifiedTweets: RDD[ClassifiedTweet] = rdd.map { status =>
            ClassifiedTweet(status.getText, true, 0.99)
          }
          storage.value.saveTweets(classifiedTweets.collect)
            .onComplete {
              case Success(tweets) =>
                pprint.pprintln("Tweets saved properly!")
                tweets.foreach(pprint.pprintln(_))
              case Failure(_) => pprint.pprintln("Error saving tweets :_(")
            }
        }
      }
    streamingContext.start()
    streamingContext.awaitTermination()
  }

  pprint.pprintln("Initializing...")
  private val twitterCredentials = loadTwitterCredentials()
  private val firebaseCredentials = loadFirebaseCredentials()
  (firebaseCredentials, twitterCredentials) match {
    case (Some(_), Some(twitterConfig)) => {
      val configuration = new ConfigurationBuilder()
        .setOAuthConsumerKey(twitterConfig.consumerKey)
        .setOAuthConsumerSecret(twitterConfig.consumerSecret)
        .setOAuthAccessToken(twitterConfig.accessToken)
        .setOAuthAccessTokenSecret(twitterConfig.accessTokenSecret)
        .build()
      val authorization = new OAuthAuthorization(configuration)
      startStreaming(authorization)
      pprint.pprintln("Application finished")
    }
    case _ => pprint.pprintln("Finishing application")
  }

}
