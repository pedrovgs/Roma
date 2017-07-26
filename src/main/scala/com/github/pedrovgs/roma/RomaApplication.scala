package com.github.pedrovgs.roma

import com.github.pedrovgs.roma.config.{ConfigLoader, TwitterConfig}
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.twitter.TwitterUtils
import twitter4j.Status
import twitter4j.auth.{Authorization, OAuthAuthorization}
import twitter4j.conf.ConfigurationBuilder

object RomaApplication extends SparkApp {

  val appName: String = "Roma"

  private def twitterStream(authorization: Authorization) =
    TwitterUtils.createFilteredStream(streamingContext, Some(authorization))

  private def loadCredentials(): Option[TwitterConfig] = {
    pprint.pprintln("Loading Twitter configuration")
    ConfigLoader.loadTwitterConfig() match {
      case Some(twitterConfig) => {
        pprint.pprintln("Configuration loaded: " + twitterConfig)
        Some(twitterConfig)
      }
      case None => {
        pprint.pprintln("Configuration couldn't be loaded. Review your resources/application.conf file")
        None
      }
    }
  }

  private def startStreaming(authorization: Authorization) = {
    pprint.pprintln("Let's start reading tweets!")
    twitterStream(authorization)
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
  private val twitterCredentials = loadCredentials()
  twitterCredentials match {
    case Some(twitterConfig) => {
      val configuration = new ConfigurationBuilder()
        .setOAuthConsumerKey(twitterConfig.consumerKey)
        .setOAuthConsumerSecret(twitterConfig.consumerSecret)
        .setOAuthAccessToken(twitterConfig.accessToken)
        .setOAuthAccessTokenSecret(twitterConfig.accessTokenSecret)
        .build()
      val authorization = new OAuthAuthorization(configuration)
      startStreaming(authorization)
      pprint.pprintln("Application finished")
    }
    case _ => pprint.pprintln("Finishing application")
  }

}
