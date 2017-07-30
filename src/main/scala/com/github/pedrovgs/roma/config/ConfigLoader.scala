package com.github.pedrovgs.roma.config

import com.typesafe.config.ConfigFactory

import scala.util.Try

case class TwitterConfig(consumerKey: String, consumerSecret: String, accessToken: String, accessTokenSecret: String)

case class FirebaseConfig(databaseUrl: String)

object ConfigLoader {

  private val config = ConfigFactory.load

  def loadTwitterConfig(): Option[TwitterConfig] = {
    Try(
      TwitterConfig(
        config.getString("roma.twitter4j.oauth.consumerKey"),
        config.getString("roma.twitter4j.oauth.consumerSecret"),
        config.getString("roma.twitter4j.oauth.accessToken"),
        config.getString("roma.twitter4j.oauth.accessTokenSecret")
      )).toOption
  }

  def loadFirebaseConfig(): Option[FirebaseConfig] = {
    Try {
      val databaseUrl = config.getString("roma.firebase.databaseUrl")
      FirebaseConfig(databaseUrl)
    }.toOption
  }

}
