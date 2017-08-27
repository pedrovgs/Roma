package com.github.pedrovgs.roma.machinelearning

import com.github.pedrovgs.roma.Console._
import com.github.pedrovgs.roma.extensions.StringExtension._
import com.github.pedrovgs.roma.machinelearning.Config._
import org.apache.spark.ml.feature.{HashingTF, IDF, StopWordsRemover}
import org.apache.spark.sql.DataFrame
import org.apache.spark.storage.StorageLevel

object FeaturesExtractor {

  def extract(tweets: DataFrame): DataFrame = {
    import org.apache.spark.sql.functions._
    print("Extracting relevant words.")
    val udfExtractTweetWords = udf(extractTweetWords)
    val tweetsWithExtractedWords = tweets.withColumn(tweetWordsColumnName, udfExtractTweetWords(col(tweetContentColumnName)))
    print("Removing stop words.")
    val stopWordsRemover = new StopWordsRemover()
    val relevantTweetWords = stopWordsRemover.setInputCol(tweetWordsColumnName).setOutputCol(relevantWrods).transform(tweetsWithExtractedWords).persist(StorageLevel.MEMORY_ONLY)
    print("Extracting features from tweet words.")
    extractTweetsFeatures(relevantTweetWords)
  }

  private val extractTweetWords: String => Array[String] =
    _.toLowerCase.trim
      .split(" ")
      .filterNot(_.startsWith("@"))
      .filterNot(_.startsWith("http"))
      .map(_.replaceAll("#", ""))
      .map(_.removeConsecutiveChars())
      .map(_.replaceAll("\\p{P}(?=\\s|$)", ""))
      .filterNot(_.isEmpty)

  private def extractTweetsFeatures(tweets: DataFrame): DataFrame = {
    val featuredTweets = new HashingTF()
      .setInputCol(relevantWrods)
      .setOutputCol(rawFeaturesColumnName)
      .transform(tweets)
    val idfModel = new IDF().setInputCol(rawFeaturesColumnName).setOutputCol(featuresColumnName).fit(featuredTweets)
    idfModel.transform(featuredTweets).persist(StorageLevel.MEMORY_ONLY)
  }
}
