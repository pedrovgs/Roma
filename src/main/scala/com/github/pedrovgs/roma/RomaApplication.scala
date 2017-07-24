package com.github.pedrovgs.roma

import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.twitter.TwitterUtils
import twitter4j.{FilterQuery, Status}

import scala.io.Source


object RomaApplication extends SparkApp {

  private val tweetsFilter = {
    val filter = new FilterQuery()
    filter.language("es")
    Some(filter)
  }

  private val twitterStream =
    TwitterUtils.createFilteredStream(streamingContext, None, tweetsFilter)

  private def loadCredential(): Unit = {
    val lines: Iterator[String] = Source.fromFile("twitter4j.properties").getLines()
    val props = lines.map(line => line.split("=")).map { case (scala.Array(k, v)) => (k, v) }
    props.foreach {
      case (k: String, v: String) => System.setProperty(k, v)
    }
  }

  private def startStreaming() = {
    twitterStream.foreachRDD { rdd: RDD[Status] =>
      pprint.pprintln(rdd.count())
    }
    streamingContext.start()
    streamingContext.awaitTermination()
  }

  pprint.pprintln("Initializing...")
  loadCredential()
  pprint.pprintln("Credentials initialized properly.")
  pprint.pprintln("Let's start reading tweets!")
  startStreaming()
}
