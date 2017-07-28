package com.github.pedrovgs.roma.config

import com.typesafe.config.ConfigFactory

import scala.util.Try

case class TwitterConfig(consumerKey: String, consumerSecret: String, accessToken: String, accessTokenSecret: String)

case class FirebaseConfig(databaseUrl: String)

object ConfigLoader {

  def loadTwitterConfig(): Option[TwitterConfig] = {
    val config = ConfigFactory.load
    Try(
      TwitterConfig(
        config.getString("twitter4j.oauth.consumerKey"),
        config.getString("twitter4j.oauth.consumerSecret"),
        config.getString("twitter4j.oauth.accessToken"),
        config.getString("twitter4j.oauth.accessTokenSecret")
      )).toOption
  }

  def loadFirebaseConfig(): Option[FirebaseConfig] = {
    val config = ConfigFactory.load
    Try(FirebaseConfig(config.getString("firebase.databaseUrl"))).toOption
  }

}
