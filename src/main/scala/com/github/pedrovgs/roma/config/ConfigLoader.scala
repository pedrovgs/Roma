package com.github.pedrovgs.roma.config

import com.typesafe.config.ConfigFactory

import scala.util.{Success, Try}

case class TwitterConfig(consumerKey: String, consumerSecret: String, accessToken: String, accessTokenSecret: String)

object ConfigLoader {

  private val consumerKeyConfigKey       = "twitter4j.oauth.consumerKey"
  private val consumerSecretConfigKey    = "twitter4j.oauth.consumerSecret"
  private val accessTokenConfigKey       = "twitter4j.oauth.accessToken"
  private val accessTokenSecretConfigKey = "twitter4j.oauth.accessTokenSecret"

  def loadTwitterConfig(): Option[TwitterConfig] = {
    val config = ConfigFactory.load
    Try(
      TwitterConfig(
        config.getString(consumerKeyConfigKey),
        config.getString(consumerSecretConfigKey),
        config.getString(accessTokenConfigKey),
        config.getString(accessTokenSecretConfigKey)
      )).toOption
  }

}
