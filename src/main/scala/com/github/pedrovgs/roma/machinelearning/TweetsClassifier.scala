package com.github.pedrovgs.roma.machinelearning

import com.github.pedrovgs.roma.Console.{print, smallSeparator}
import com.github.pedrovgs.roma.TweetColumns._
import org.apache.spark.mllib.classification.SVMModel
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.sql.{DataFrame, SQLContext}

object TweetsClassifier {
  def classify(sqlContext: SQLContext, model: SVMModel, tweets: DataFrame): DataFrame = {
    import sqlContext.implicits._
    print("Classifying tweets.")
    smallSeparator()
    val classification = tweets.rdd
      .map { row =>
        val id         = row.getAs[Long](tweetIdColumnName)
        val feature    = row.getAs[Vector](featuresColumnName)
        val prediction = model.predict(feature)
        (id, prediction)
      }
      .toDF(tweetIdColumnName, classificationColumnName)
    print("Classification finished. Let's review the result:")
    val classifiedTweets = tweets.join(classification, tweetIdColumnName)
    classifiedTweets.show()
    classifiedTweets
  }
}
