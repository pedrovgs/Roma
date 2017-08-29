package com.github.pedrovgs.roma.machinelearning

import com.github.pedrovgs.roma.Console._
import com.github.pedrovgs.roma.extensions.StringExtension._
import com.github.pedrovgs.roma.TweetColumns._
import com.vdurmont.emoji.EmojiParser
import org.apache.spark.ml.feature.{HashingTF, IDF, StopWordsRemover}
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.sql.DataFrame

object FeaturesExtractor {

  def extract(tweets: DataFrame): DataFrame = {
    import org.apache.spark.sql.functions._
    print("Extracting relevant words.")
    val udfExtractTweetWords = udf(extractTweetWords)
    val tweetsWithExtractedWords =
      tweets.withColumn(tweetWordsColumnName, udfExtractTweetWords(col(tweetContentColumnName)))
    print("Removing stop words.")
    val stopWordsRemover = new StopWordsRemover()
    val relevantTweetWords = stopWordsRemover
      .setInputCol(tweetWordsColumnName)
      .setOutputCol(relevantWords)
      .transform(tweetsWithExtractedWords)
      .cache()
    print("Extracting features from tweet words.")
    extractTweetsFeatures(relevantTweetWords)
  }

  private val extractTweetWords: String => Array[String] =
    _.toLowerCase.trim
      .split(" ")
      .filterNot(_.startsWith("@"))
      .filterNot(_.startsWith("http"))
      .map(EmojiParser.removeAllEmojis)
      .map(_.replaceAll("#", ""))
      .map(_.removeConsecutiveChars())
      .map(_.replaceAll("\\p{P}(?=\\s|$)", ""))
      .filterNot(_.isEmpty)

  private def extractTweetsFeatures(tweets: DataFrame): DataFrame = {
    import org.apache.spark.sql.functions._
    val featuredTweets = new HashingTF()
      .setInputCol(relevantWords)
      .setOutputCol(rawFeaturesColumnName)
      .transform(tweets)
    val idfModel  = new IDF().setInputCol(rawFeaturesColumnName).setOutputCol(featuresColumnName).fit(featuredTweets)
    val idfTweets = idfModel.transform(featuredTweets)
    MLUtils
      .convertVectorColumnsFromML(idfTweets, featuresColumnName)
      .cache()
      .withColumn(tweetIdColumnName, monotonically_increasing_id())
  }
}
