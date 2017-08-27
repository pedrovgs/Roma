package com.github.pedrovgs.roma.machinelearning

import com.github.pedrovgs.roma.Console._
import com.github.pedrovgs.roma.machinelearning.Config._
import com.github.pedrovgs.roma.{Resources, SparkApp}
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.sql.DataFrame
import org.apache.spark.storage.StorageLevel

object RomaMachineLearningTrainer extends SparkApp with Resources {

  override val appName = "RomaMachineLearningTrainer"

  override def main(args: Array[String]): Unit = {
    val (trainingTweets, testingTweets) = readCorpusTweetsAndExtractFeatures
    val model = trainSvmModel(trainingTweets)
    measureTraining(model, trainingTweets, "Training tweets")
    measureTraining(model, testingTweets, "Testing tweets")
  }

  private def readCorpusTweetsAndExtractFeatures = {
    print("Reading tweets corpus.")
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

  private def trainSvmModel(tweets: DataFrame) = {
    print("Training our Support Vector Machine model.")
    separator()
    print(
      "Training Support Vector Machine model with " + numberOfIterations + " number of iterations and " + tweets
        .count() + " training tweets.")

    val featuredTweets = MLUtils.convertVectorColumnsFromML(tweets, featuresColumnName)
    val labeledPoints = featuredTweets.rdd.map { row =>
      val label = row.getAs[Double](labelColumnName)
      val features = row.getAs[Vector](featuresColumnName)
      new LabeledPoint(label, features)
    }.persist(StorageLevel.MEMORY_ONLY)
    val model = SVMWithSGD.train(labeledPoints, numberOfIterations)
    model.clearThreshold()
    print("Model trained properly!")
    model
  }

  def measureTraining(model: SVMModel, tweets: DataFrame, name: String): Unit = {
    print("Measuring how the model has been trained using the dataset named: " + name)
    separator()
  }

}
