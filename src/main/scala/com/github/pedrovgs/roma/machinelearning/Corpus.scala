package com.github.pedrovgs.roma.machinelearning

import com.github.pedrovgs.roma.Resources
import com.github.pedrovgs.roma.TweetColumns._
import com.github.pedrovgs.roma.machinelearning.FeaturesExtractor._
import org.apache.spark.sql.{DataFrame, SQLContext}

private[machinelearning] object Corpus extends Resources {

  private val contentColumnName         = "content"
  private val sentimentColumnName       = "sentiment"
  private val positiveSentimentCsvValue = 4
  private val negativeSentimentCsvValue = 0
  private val positiveLabel             = 1.0
  private val negativeLabel             = 0.0

  def trainingTweets(sqlContext: SQLContext): DataFrame = readAndFilterTweets("/training.gz", sqlContext)

  def testingTweets(sqlContext: SQLContext): DataFrame = readAndFilterTweets("/test.csv", sqlContext)

  private def readAndFilterTweets(resourceName: String, sqlContext: SQLContext): DataFrame = {
    val tweets: DataFrame = readTweets(resourceName, sqlContext)
    extract(tweets).cache()
  }

  private def readTweets(resourceName: String, sqlContext: SQLContext) = {
    import sqlContext.implicits._
    sqlContext.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv(getFilePath(resourceName))
      .filter { row =>
        val sentiment = row.getAs[Int](sentimentColumnName)
        sentiment == negativeSentimentCsvValue || sentiment == positiveSentimentCsvValue
      }
      .map { row =>
        val label = if (row.getAs[Int](sentimentColumnName).equals(positiveSentimentCsvValue)) {
          positiveLabel
        } else {
          negativeLabel
        }
        (label, row.getAs[String](contentColumnName))
      }
      .toDF(labelColumnName, tweetContentColumnName)
  }

}
