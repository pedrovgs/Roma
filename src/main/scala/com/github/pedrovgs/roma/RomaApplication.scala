package com.github.pedrovgs.roma

import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.twitter.TwitterUtils
import twitter4j.Status

import scala.io.Source


object RomaApplication extends SparkApp {

  val appName: String = "Roma"

  private val twitterStream = TwitterUtils.createFilteredStream(streamingContext, None)

  private def loadCredentials(): Unit = {
    val lines: Iterator[String] = Source.fromFile("twitter4j.properties").getLines()
    val props = lines.map(line => line.split("=")).map { case (scala.Array(k, v)) => (k, v) }
    props.foreach {
      case (k: String, v: String) => System.setProperty(k, v)
    }
  }

  private def startStreaming() = {
    twitterStream
      .filter(_.getLang == "en")
      .foreachRDD { rdd: RDD[Status] =>
        if (!rdd.isEmpty()) {
          pprint.pprintln("Let's analyze a bunch of tweets!")
        }
        rdd.foreach { tweet =>
          pprint.pprintln(tweet.getText)
        }
      }
    streamingContext.start()
    streamingContext.awaitTermination()
  }

  pprint.pprintln("Initializing...")
  loadCredentials()
  pprint.pprintln("Credentials initialized properly.")
  pprint.pprintln("Let's start reading tweets!")
  startStreaming()
  pprint.pprintln("Application finished")

}
