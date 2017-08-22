package com.github.pedrovgs.roma

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.log4j.{Level, Logger}

private[roma] trait SparkApp extends App {

  val appName: String

  private lazy val conf: SparkConf =
    new SparkConf()
      .set("spark.kryoserializer.buffer.mb", "256")
      .set("spark.kryoserializer.buffer.max", "512")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

  private lazy val sparkSession: SparkSession = SparkSession
    .builder()
    .appName(appName)
    .config(conf)
    .master(masterUrl())
    .getOrCreate()
  lazy val sparkContext: SparkContext = sparkSession.sparkContext
  lazy val sqlContext: SQLContext     = sparkSession.sqlContext
  lazy val streamingContext: StreamingContext = {
    val streamingContext = new StreamingContext(sparkContext, Seconds(1))
    streamingContext.checkpoint("./checkpoint")
    streamingContext
  }

  lazy val objectMapper: ObjectMapper = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
  }

  private def masterUrl(): String = {
    Logger.getRootLogger.setLevel(Level.ERROR)
    val defaultMasterUrl = "local[*]"
    if (args == null || args.isEmpty) {
      defaultMasterUrl
    } else {
      Option(args(0)).getOrElse(defaultMasterUrl)
    }
  }

}
