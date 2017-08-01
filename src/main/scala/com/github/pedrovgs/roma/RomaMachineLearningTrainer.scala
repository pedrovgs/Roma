package com.github.pedrovgs.roma

import org.apache.spark.ml.feature.{Word2Vec, Word2VecModel}
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.linalg.{DenseVector, Vector}
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

  private val trainingTweets: DataFrame = readAndFilterTweets("/training.csv").cache()
  private val testTweets                = readAndFilterTweets("/test.csv").cache()
  private val word2VectorModel = new Word2Vec()
    .setInputCol(tweetWordsColumnName)
    .setOutputCol(featuresColumnName)
    .fit(trainingTweets)
  private val featuredTrainingTweets = tokenizeTweets(trainingTweets, word2VectorModel).cache()
  private val featuredTestTweets     = tokenizeTweets(trainingTweets, word2VectorModel).cache()

  private val svmModel     = trainSvmModel(featuredTrainingTweets, numberOfIterations)
  private val modelMetrics = measureSvmModelTraining(featuredTestTweets, svmModel)

  predict(svmModel,
          sparkContext.parallelize(
            Seq("I'm so happy today! It's my birthday!", "Sad to see a new flight delayed again :_(")))

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
      .map { row =>
        val label = if (row.getAs[Int](sentimentColumnName).equals(4)) {
          1.0
        } else {
          0.0
        }
        val content = row.getAs[String]("content").toLowerCase.split(" ")
        (label, content)
      }
      .toDF(labelColumnName, tweetWordsColumnName)
  }

  private def tokenizeTweets(tweets: DataFrame, word2VectorModel: Word2VecModel): RDD[LabeledPoint] = {
    pprint.pprintln("Tokenizing " + tweets.count() + " tweets into labeled points.")
    val tokenizedTweets = word2VectorModel.transform(tweets)
    val featuredTweets  = MLUtils.convertVectorColumnsFromML(tokenizedTweets, featuresColumnName)
    featuredTweets.rdd.map { row =>
      val label    = row.getAs[Double](labelColumnName)
      val features = row.getAs[DenseVector](featuresColumnName)
      new LabeledPoint(label, features)
    }
  }

  private def trainSvmModel(labeledPoints: RDD[LabeledPoint], numberOfIterations: Int) = {
    pprint.pprintln(
      "Training SVM model with " + numberOfIterations + " number of iterations and " + labeledPoints
        .count() + " training tweets.")
    SVMWithSGD.train(labeledPoints, numberOfIterations).clearThreshold()
  }

  private def measureSvmModelTraining(testData: RDD[LabeledPoint], svmModel: SVMModel) = {
    val scoreAndLabels =
      testData.map { point =>
        val score = svmModel.predict(point.features)
        (score, point.label)
      }
    val metrics = new BinaryClassificationMetrics(scoreAndLabels)
    pprint.pprintln("Model training completed. These are the result:")
    pprint.pprintln("Area under PR: " + metrics.areaUnderPR())
    pprint.pprintln("Area under ROC: " + metrics.areaUnderROC())
    metrics
  }

  def predict(svmModel: SVMModel, originalTweets: RDD[String]) = {
    pprint.pprintln("Let's analyze some tweets using Support Vector Machine!")
    pprint.pprintln("List of original tweets:")
    originalTweets.foreach(pprint.pprintln(_))
    val tweetsToAnalyze = originalTweets
      .map(_.split(" "))
      .toDF(tweetWordsColumnName)
    val tokenizedTweets = word2VectorModel.transform(tweetsToAnalyze)
    val convertedEarthquakeTweetsResult =
      MLUtils.convertVectorColumnsFromML(tokenizedTweets, featuresColumnName)
    val earthquakeTweetsData: RDD[Vector] =
      convertedEarthquakeTweetsResult.rdd.map(_.getAs[DenseVector](featuresColumnName))

    val prediction = svmModel.predict(earthquakeTweetsData)

    val predictionResult: RDD[(String, Double)] = originalTweets.zip(prediction)
    pprint.pprintln(
      "The following table shows the result of a tweet prediction based on a Support Vector Machine algorithm and using tweets as input data:")
    pprint.pprintln(
      predictionResult
        .map(tuple => "Tweet: " + tuple._1 + " - Probability: " + tuple._2)
        .collect())

  }

}
