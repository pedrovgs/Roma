package com.github.pedrovgs.roma

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

private[roma] trait SparkApp extends App {

  private val batchDuration = Seconds(15)

  val appName: String

  private lazy val conf: SparkConf =
    new SparkConf()
      .set("spark.streaming.backpressure.enabled", "true")

  private lazy val sparkSession: SparkSession = SparkSession
    .builder()
    .appName(appName)
    .config(conf)
    .master("local[*]")
    .getOrCreate()
  lazy val sparkContext: SparkContext = sparkSession.sparkContext
  lazy val sqlContext: SQLContext     = sparkSession.sqlContext
  lazy val streamingContext: StreamingContext = {
    val streamingContext = new StreamingContext(sparkContext, batchDuration)
    streamingContext.checkpoint("./checkpoint")
    streamingContext
  }

  lazy val objectMapper: ObjectMapper = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
  }

}
