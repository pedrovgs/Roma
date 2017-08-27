package com.github.pedrovgs.roma.machinelearning

import com.github.pedrovgs.roma.extensions.StringExtension._
import com.github.pedrovgs.roma.{Resources, SparkApp}
import org.apache.spark.ml.feature._
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

  private val numberOfIterations: Int     = 100
  private val curatedTweetWordsColumnName = "tweetWords"
  private val tweetWordsColumnName        = "words"
  private val sentimentColumnName         = "sentiment"
  private val rawFeaturesColumnName       = "rawFeatures"
  private val featuresColumnName          = "features"
  private val labelColumnName             = "label"

  pprint.pprintln("Let's read some tweets for our training process")

  private val trainingTweets: DataFrame = readAndFilterTweets("/training.gz").cache()
  private val testTweets: DataFrame     = readAndFilterTweets("/test.csv").cache()

  pprint.pprintln("Here we have some training tweets already prepared to extract features")
  trainingTweets.show()
  pprint.pprintln("Here we have some test tweets already prepared to extract features")
  testTweets.show()

  pprint.pprintln("Let's extract features from the Tweets content")

  private val featurizedData = new HashingTF()
    .setInputCol(curatedTweetWordsColumnName)
    .setOutputCol(rawFeaturesColumnName)
    .transform(trainingTweets)
  private val featurizedTestData = new HashingTF()
    .setInputCol(curatedTweetWordsColumnName)
    .setOutputCol(rawFeaturesColumnName)
    .transform(testTweets)

  private val idfModel =
    new IDF().setInputCol(rawFeaturesColumnName).setOutputCol(featuresColumnName).fit(featurizedData)

  private val featuredTrainingTweets = featurizeTweets(featurizedData).cache()
  private val featuredTestTweets     = featurizeTweets(featurizedTestData).cache()

  pprint.pprintln("Ready to start training our Support Vector Machine model!")
  private val svmModel = trainSvmModel(featuredTrainingTweets, numberOfIterations)

  pprint.pprintln("Model ready! Let's measure our area under PR & ROC for the TESTING data")
  private val testScoreAndLabels = measureSvmModelTraining(featuredTestTweets, svmModel)
  pprint.pprintln("Model ready! Let's measure the area under PR & ROC for the TRAINING data")
  measureSvmModelTraining(featuredTrainingTweets, svmModel)

  pprint.pprintln("Time to use our model!")
  predict(
    svmModel,
    sparkContext.parallelize(
      Seq(
        "I'm happy to announce I've found a new excelent job!",
        "I love u",
        "Such a great news!",
        "I really love love love love love love you my darling",
        "I don't understand why airlines are so incompetent",
        "This is a neutral tweet",
        "My house is big. Yours is small",
        "This is the worst movie I've ever seen",
        "I hate you",
        "I really hate hate hate hate hate hate you son of a bitch"
      ))
  )

  private def readAndFilterTweets(resourceName: String): DataFrame = {
    pprint.pprintln("Reading tweets from: " + resourceName)
    val stopWordsRemover = new StopWordsRemover()
    val tweets = sqlContext.read
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
        //Sentiment 4 in the CSV is a POSITIVE sentiment. Labeled as 1
        //Sentiment 0 in the CSV is a NEGATIVE sentiment. Labeled as 0
        val label = if (row.getAs[Int](sentimentColumnName).equals(4)) {
          1.0
        } else {
          0.0
        }
        val tweetWords: Array[String] = extractTweetWords(row.getAs[String]("content"))
        (label, tweetWords)
      }
      .toDF(labelColumnName, tweetWordsColumnName)
    stopWordsRemover.setInputCol(tweetWordsColumnName).setOutputCol(curatedTweetWordsColumnName).transform(tweets)
  }

  private def extractTweetWords(tweet: String): Array[String] =
    tweet.toLowerCase.trim
      .split(" ")
      .filterNot(_.startsWith("@"))
      .filterNot(_.startsWith("http"))
      .map(_.replaceAll("#", ""))
      .map(_.removeConsecutiveChars)
      .map(_.replaceAll("\\p{P}(?=\\s|$)", ""))
      .filterNot(_.isEmpty)

  private def featurizeTweets(tweets: DataFrame): RDD[LabeledPoint] = {
    pprint.pprintln("Featuring " + tweets.count() + " tweets into labeled points.")
    val tokenizedTweets = idfModel.transform(tweets)
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
    val regParam = 0.01
    val stepSize = 1.0
    val model    = SVMWithSGD.train(labeledPoints, numberOfIterations, stepSize, regParam)
    model.clearThreshold()
  }

  private def measureSvmModelTraining(testData: RDD[LabeledPoint], svmModel: SVMModel): RDD[(Double, Double)] = {
    pprint.pprintln("Labeled point dimension = " + testData.first().features.size)
    //pprint.pprintln("First feature point = " + testData)
    val scoreAndLabels =
      testData.map { point =>
        val score = svmModel.predict(point.features)
        (score, point.label)
      }
    pprint.pprintln("Dimension of the weights =" + svmModel.weights.size)
    pprint.pprintln("Maximum value of the weights=" + svmModel.weights.asML.toArray.max)
    pprint.pprintln("Minimum value of the weights=" + svmModel.weights.asML.toArray.min)
    pprint.pprintln("Intercept =" + svmModel.intercept)
    //pprint.pprintln("Summary of the Model" + svmModel.toString())

    val metrics = new BinaryClassificationMetrics(scoreAndLabels)
    pprint.pprintln("--------------------------------------")
    pprint.pprintln("Score and Labels:")
    scoreAndLabels.take(5).foreach {
      case (score, label) => pprint.pprintln("Score: " + score + ", label: " + label)
    }
    pprint.pprintln("Binary classification metrics:")
    pprint.pprintln("Area under PR: " + metrics.areaUnderPR())
    pprint.pprintln("Area under ROC: " + metrics.areaUnderROC())
    //metrics.pr()

    val scores   = scoreAndLabels.map(_._1).cache()
    val labels   = scoreAndLabels.map(_._2).cache()
    val minScore = scores.min()
    pprint.pprintln("Min score -> " + minScore)
    val maxScore = scores.max()
    pprint.pprintln("Max score -> " + maxScore)
    val argMaxScore = scores.filter(_ == maxScore)

    val positiveValues = scores.filter(_ > 0).count()
    val negativeValues = scores.filter(_ < 0).count()

    val truePositives = scoreAndLabels
      .filter {
        case (score, label) => score > 0 && label == 1.0
      }
      .count()
    val falsePositives = scoreAndLabels
      .filter {
        case (score, label) => score > 0 && label == 0.0
      }
      .count()
    val trueNegative = scoreAndLabels
      .filter {
        case (score, label) => score <= 0 && label == 0.0
      }
      .count()
    val falseNegative = scoreAndLabels
      .filter {
        case (score, label) => score <= 0 && label == 1.0
      }
      .count()

    val sumPredictedPositives = truePositives + falsePositives
    val sumPredictedNegatives = trueNegative + falseNegative

    val sumRealPositives = truePositives + falseNegative
    val sumRealNegatives = trueNegative + falsePositives

    val sumTotal = sumRealNegatives + sumRealPositives

    val percWellClassified = (truePositives.toDouble / sumTotal.toDouble) * 100.0 + (trueNegative.toDouble / sumTotal.toDouble) * 100.0

    pprint.pprintln("Positive scores -> " + positiveValues)
    pprint.pprintln("Positive labels -> " + labels.filter(_ == 1.0).count())
    pprint.pprintln("Negative scores -> " + negativeValues)
    pprint.pprintln("Negative labels -> " + labels.filter(_ == 0.0).count())

    pprint.pprintln("Let's write the confusion matrix")
    pprint.pprintln("  ______________________ |")
    pprint.pprintln("        Prediction        |")
    pprint.pprintln("  ______________________ |")
    pprint.pprintln("         | P | N | Sum")
    pprint.pprintln("Real | P |" + truePositives + "|" + falseNegative + "| " + sumRealPositives)
    pprint.pprintln("     | N |" + falsePositives + "|" + trueNegative + "|" + sumRealNegatives)
    pprint.pprintln("     |Sum|" + sumPredictedPositives + "|" + sumPredictedNegatives + "|" + sumTotal)

    pprint.pprintln("")
    pprint.pprintln("Let's write the confusion matrix in percentage")
    pprint.pprintln("  ______________________ |")
    pprint.pprintln("        Prediction        |")
    pprint.pprintln("  ______________________ |")
    pprint.pprintln("         | P | N ")
    pprint.pprintln(
      "Real | P |" + (truePositives.toDouble / sumTotal.toDouble) * 100.0 + " % |" + (falseNegative.toDouble / sumTotal.toDouble) * 100.0 + " %")
    pprint.pprintln(
      "     | N |" + (falsePositives.toDouble / sumTotal.toDouble) * 100.0 + " % |" + (trueNegative.toDouble / sumTotal.toDouble) * 100.0 + " %")
    pprint.pprintln("Percentage of well classified: " + percWellClassified + " %")

    pprint.pprintln("--------------------------------------")

    val pointsOverMax           = scoreAndLabels.filter(_._1 >= 0.3)
    val pointsLowerMin          = scoreAndLabels.filter(_._1 <= -0.3)
    val wellPositivelyPredicted = pointsOverMax.filter(_._2 == 1.0)
    val wellNegativelyPredicted = pointsLowerMin.filter(_._2 == 0.0)

    pprint.pprintln("Positive predicted properly with a 0.3 as threshold = " + wellPositivelyPredicted.count())
    pprint.pprintln("Negative predicted properly with a -0.3 as threshold = " + wellNegativelyPredicted.count())

    pprint.pprintln("--------------------------------------")
    scoreAndLabels
  }

  private def predict(svmModel: SVMModel, originalTweets: RDD[String]) = {
    pprint.pprintln("List of original tweets:")
    originalTweets.foreach(pprint.pprintln(_))
    val stopWordsRemover = new StopWordsRemover()
    val tweets = originalTweets
      .map(extractTweetWords)
      .toDF(tweetWordsColumnName)
    val tweetsToAnalyze =
      stopWordsRemover.setInputCol(tweetWordsColumnName).setOutputCol(curatedTweetWordsColumnName).transform(tweets)
    val featurizedData = new HashingTF()
      .setInputCol(curatedTweetWordsColumnName)
      .setOutputCol(rawFeaturesColumnName)
      .transform(tweetsToAnalyze)
    val featurizedTweets = idfModel.transform(featurizedData)
    val convertedTweetsResult =
      MLUtils.convertVectorColumnsFromML(featurizedTweets, featuresColumnName)
    val tweetsData: RDD[Vector] =
      convertedTweetsResult.rdd.map(_.getAs[Vector](featuresColumnName))

    val prediction = svmModel.predict(tweetsData).cache()

    val predictionResult: RDD[(String, Double)] = originalTweets.zip(prediction).cache()
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
