package com.github.pedrovgs.roma

import org.apache.spark.streaming.twitter.TwitterUtils
import twitter4j.FilterQuery

object RomaApplication extends SparkApp {

  private val tweetsFilter = {
    val filter = new FilterQuery()
    filter.language("es")
  }

  private val twitterStream = TwitterUtils.createFilteredStream(streamingContext, None)

  streamingContext.start()
  streamingContext.awaitTermination()
}
