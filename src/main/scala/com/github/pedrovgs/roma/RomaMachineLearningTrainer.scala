package com.github.pedrovgs.roma

import org.apache.spark.ml.feature.{Word2Vec, Word2VecModel}
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame

object RomaMachineLearningTrainer extends SparkApp with Resources {

  override val appName = "RomaMachineLearningTrainer"

  import sqlContext.implicits._

  private val numberOfIterations: Int = 100
  private val tweetWordsColumnName    = "tweetWords"
  private val sentimentColumnName     = "sentiment"
  private val featuresColumnName      = "features"
  private val labelColumnName         = "label"

  pprint.pprintln("Let's read some tweets for our trainig process")

  private val trainingTweets: DataFrame = readAndFilterTweets("/training.csv").cache()
  private val testTweets: DataFrame     = readAndFilterTweets("/test.csv").cache()

  pprint.pprintln("Here we have some training tweets already prepared to extract features")
  trainingTweets.show()
  pprint.pprintln("Here we have some test tweets already prepared to extract features")
  testTweets.show()

  pprint.pprintln("Let's extract features from the Tweets content")
  private val word2VectorModel = new Word2Vec()
    .setInputCol(tweetWordsColumnName)
    .setOutputCol(featuresColumnName)
    .fit(trainingTweets)
  private val featuredTrainingTweets = featurizeTweets(trainingTweets, word2VectorModel).cache()
  private val featuredTestTweets     = featurizeTweets(testTweets, word2VectorModel).cache()

  pprint.pprintln("Ready to start training our Support Vector Machine model!")
  private val svmModel     = trainSvmModel(featuredTrainingTweets, numberOfIterations)
  pprint.pprintln("Model ready! Let's measure our area under PR & ROC")
  measureSvmModelTraining(featuredTestTweets, svmModel)

  pprint.pprintln("Time to use our model!")
  predict(svmModel,
          sparkContext.parallelize(
            Seq("I'm happy to announce I've found a new excelent job!",
              "I don't understand why airlines are so incompetent")))

  private def readAndFilterTweets(resourceName: String): DataFrame = {
    pprint.pprintln("Reading tweets from: " + resourceName)
    sqlContext.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv(getFilePath(resourceName))
      .filter { row =>
        val sentiment = row.getAs[Int](sentimentColumnName)
        sentiment == 0 || sentiment == 4
      }
      .rdd
      .filter { row =>
        val sentiment = row.getAs[Int](sentimentColumnName)
        sentiment == 4 || sentiment == 0
      }
      .map { row =>
        //Sentiment 4 in the CSV is a positive sentiment
        //Sentiment 0 in the CSV is a NEGATIVE sentiment
        val label = if (row.getAs[Int](sentimentColumnName).equals(4)) {
          1.0
        } else {
          0.0
        }
        val tweetWords: Array[String] = extractTweetWords(row.getAs[String]("content"))
        (label, tweetWords)
      }
      .toDF(labelColumnName, tweetWordsColumnName)
  }

  private def extractTweetWords(tweet: String): Array[String] = tweet.toLowerCase.split(" ")
    .map(_.replaceAll("""([\p{Punct}&&[^.]]|\b\p{IsLetter}{1,2}\b)\s*""", ""))
    .filterNot(_.startsWith("@"))
    .filterNot(_.isEmpty)

  private def featurizeTweets(tweets: DataFrame, word2VectorModel: Word2VecModel): RDD[LabeledPoint] = {
    pprint.pprintln("Featuring " + tweets.count() + " tweets into labeled points.")
    val tokenizedTweets = word2VectorModel.transform(tweets)
    val featuredTweets  = MLUtils.convertVectorColumnsFromML(tokenizedTweets, featuresColumnName)
    featuredTweets.rdd.map { row =>
      val label    = row.getAs[Double](labelColumnName)
      val features = row.getAs[Vector](featuresColumnName)
      new LabeledPoint(label, features)
    }
  }

  private def trainSvmModel(labeledPoints: RDD[LabeledPoint], numberOfIterations: Int) = {
    pprint.pprintln(
      "Training SVM model with " + numberOfIterations + " number of iterations and " + labeledPoints
        .count() + " training tweets.")
    SVMWithSGD.train(labeledPoints, numberOfIterations)
  }

  private def measureSvmModelTraining(testData: RDD[LabeledPoint], svmModel: SVMModel) = {
    val scoreAndLabels =
      testData.map { point =>
        val score = svmModel.predict(point.features)
        (score, point.label)
      }
    val metrics = new BinaryClassificationMetrics(scoreAndLabels)
    pprint.pprintln("Binary classification metrics:")
    pprint.pprintln("Area under PR: " + metrics.areaUnderPR())
    pprint.pprintln("Area under ROC: " + metrics.areaUnderROC())
    metrics
  }

  private def predict(svmModel: SVMModel, originalTweets: RDD[String]) = {
    pprint.pprintln("List of original tweets:")
    originalTweets.foreach(pprint.pprintln(_))
    val tweetsToAnalyze = originalTweets
      .map(extractTweetWords)
      .toDF(tweetWordsColumnName)
    val featurizedTweets = word2VectorModel.transform(tweetsToAnalyze)
    val convertedTweetsResult =
      MLUtils.convertVectorColumnsFromML(featurizedTweets, featuresColumnName)
    val tweetsData: RDD[Vector] =
      convertedTweetsResult.rdd.map(_.getAs[Vector](featuresColumnName))

    val prediction = svmModel.predict(tweetsData)

    val predictionResult: RDD[(String, Double)] = originalTweets.zip(prediction)
    pprint.pprintln(
      "The following table shows the result of a tweet prediction based on a Support Vector Machine algorithm and using tweets as input data:")
    pprint.pprintln("Class 0 : Angry tweet")
    pprint.pprintln("Class 1 : Happy tweet")
    pprint.pprintln(
      predictionResult
        .map(tuple => "Tweet: " + tuple._1 + " - Class: " + tuple._2)
        .collect())

  }

}
