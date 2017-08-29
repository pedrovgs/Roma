package com.github.pedrovgs.roma.config

import com.typesafe.config.ConfigFactory

import scala.util.Try

case class TwitterConfig(consumerKey: String, consumerSecret: String, accessToken: String, accessTokenSecret: String)

case class FirebaseConfig(databaseUrl: String)

case class MachineLearningConfig(numberOfIterations: Int,
                                 outputFolder: String,
                                 modelFileName: String,
                                 positiveThreshold: Double,
                                 negativeThreshold: Double)

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

  def loadMachineLearningTrainingConfig(): Option[MachineLearningConfig] = {
    Try(
      MachineLearningConfig(
        config.getInt("roma.machineLearning.numberOfIterations"),
        config.getString("roma.machineLearning.outputFolder"),
        config.getString("roma.machineLearning.modelFileName"),
        config.getDouble("roma.machineLearning.positiveThreshold"),
        config.getDouble("roma.machineLearning.negativeThreshold")
      )).toOption
  }

}
