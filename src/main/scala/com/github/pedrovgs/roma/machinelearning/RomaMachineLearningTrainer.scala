package com.github.pedrovgs.roma.machinelearning

import com.github.pedrovgs.roma.Console._
import com.github.pedrovgs.roma.TweetColumns._
import com.github.pedrovgs.roma.config.{ConfigLoader, MachineLearningConfig}
import com.github.pedrovgs.roma.{Resources, SparkApp}
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame

object RomaMachineLearningTrainer extends SparkApp with Resources {

  override val appName = "RomaMachineLearningTrainer"

  override def main(args: Array[String]): Unit = {
    super.main(args)
    val config = ConfigLoader.loadMachineLearningTrainingConfig()
    config match {
      case Some(machineLearningConfig) => trainModel(machineLearningConfig)
      case None                        => print("Review your machine learning configuration inside the file application.conf")
    }
  }

  private def trainModel(machineLearningConfig: MachineLearningConfig) = {
    val (trainingTweets, testingTweets) = readCorpusTweetsAndExtractFeatures
    val model                           = trainSvmModel(trainingTweets, machineLearningConfig)
    saveModel(model, machineLearningConfig)
    measureTraining(model, trainingTweets, "TRAINING TWEETS")
    measureTraining(model, testingTweets, "TESTING TWEETS")
    classifySomeTweets(model)
  }

  private def readCorpusTweetsAndExtractFeatures = {
    print("Reading tweets from the project corpus.")
    separator()
    print("Extracting training tweets features:")
    val trainingTweets = Corpus.trainingTweets(sqlContext)
    smallSeparator()
    trainingTweets.show()
    print("Extracting testing tweets features:")
    val testingTweets = Corpus.testingTweets(sqlContext)
    smallSeparator()
    testingTweets.show()
    (trainingTweets, testingTweets)
  }

  private def trainSvmModel(tweets: DataFrame, machineLearningConfig: MachineLearningConfig) = {
    print("Training our Support Vector Machine model.")
    separator()
    val numberOfIterations = machineLearningConfig.numberOfIterations
    print(
      "Training Support Vector Machine model with " + numberOfIterations + " number of iterations and " + tweets
        .count() + " training tweets.")

    val labeledPoints: RDD[LabeledPoint] = toLabeledPoints(tweets)
    val model                            = SVMWithSGD.train(labeledPoints, numberOfIterations)
    model.clearThreshold()
    print("Model trained properly!")
    model
  }

  private def measureTraining(model: SVMModel, tweets: DataFrame, name: String): Unit = {
    print("Measuring how the model has been trained using the dataset named: " + name + ".")
    separator()
    val classifiedTweets = TweetsClassifier.classify(sqlContext, model, tweets)
    val scoresAndLabels = classifiedTweets.rdd.map { row =>
      val score = row.getAs[Double](classificationColumnName)
      val label = row.getAs[Double](labelColumnName)
      (score, label)
    }
    smallSeparator()
    val scores   = scoresAndLabels.map(_._1).cache()
    val labels   = scoresAndLabels.map(_._2).cache()
    val minScore = scores.min()
    print("Min score -> " + minScore)
    val maxScore = scores.max()
    print("Max score -> " + maxScore)
    val positiveValuesAcc = sparkContext.longAccumulator
    val negativeValuesAcc = sparkContext.longAccumulator
    val truePositivesAcc  = sparkContext.longAccumulator
    val falsePositivesAcc = sparkContext.longAccumulator
    val trueNegativesAcc  = sparkContext.longAccumulator
    val falseNegativesAcc = sparkContext.longAccumulator
    scoresAndLabels.foreach {
      case (score, label) => {
        if (score > 0) {
          positiveValuesAcc.add(1)
        } else if (score < 0) {
          negativeValuesAcc.add(1)
        }
        if (score > 0 && label == 1.0) {
          truePositivesAcc.add(1)
        } else if (score > 0 && label == 0.0) {
          falsePositivesAcc.add(1)
        } else if (score <= 0 && label == 0.0) {
          trueNegativesAcc.add(1)
        } else if (score <= 0 && label == 1.0) {
          falseNegativesAcc.add(1)
        }
      }
    }
    val sumTruePositives                 = truePositivesAcc.value + falseNegativesAcc.value
    val sumTrueNegatives                 = trueNegativesAcc.value + falsePositivesAcc.value
    val sumTotal                         = sumTrueNegatives + sumTruePositives
    val percentageOfWellClassifiedTweets = (truePositivesAcc.value.toDouble / sumTotal.toDouble) * 100.0 + (trueNegativesAcc.value.toDouble / sumTotal.toDouble) * 100.0
    print("Positive scores -> " + positiveValuesAcc.value)
    print("Positive labels -> " + labels.filter(_ == 1.0).count())
    print("Negative scores -> " + negativeValuesAcc.value)
    print("Negative labels -> " + labels.filter(_ == 0.0).count())
    print("Percentage of well classified: " + percentageOfWellClassifiedTweets + " %")
  }

  private def toLabeledPoints(tweets: DataFrame) = {
    val labeledPoints = tweets.rdd
      .map { row =>
        val label    = row.getAs[Double](labelColumnName)
        val features = row.getAs[Vector](featuresColumnName)
        new LabeledPoint(label, features)
      }
      .cache()
    labeledPoints
  }

  private def classifySomeTweets(model: SVMModel): Unit = {
    import sqlContext.implicits._
    val tweets = sparkContext.parallelize(
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
    separator()
    print("List of original tweets:")
    val originalTweets = tweets.toDF(tweetContentColumnName)
    originalTweets.show()
    val tweetsPlusFeatures = FeaturesExtractor.extract(originalTweets)
    val classifiedTweets   = TweetsClassifier.classify(sqlContext, model, tweetsPlusFeatures)
    print("Classification finished. Remember:")
    print(" - Class NEGATIVE : Angry tweet")
    print(" - Class POSITIVE : Happy tweet")
  }

  def saveModel(model: SVMModel, config: MachineLearningConfig) = {
    model.save(sparkContext, config.outputFolder + config.modelFileName)
  }
}
