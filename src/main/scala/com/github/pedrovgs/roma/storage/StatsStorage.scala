package com.github.pedrovgs.roma.storage

import com.github.pedrovgs.roma.{ClassificationStats, ClassifiedTweet, Sentiment}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

object StatsStorage {
  private val refPath = "/classifiedTweetsStats"

  def updateStats(tweets: Seq[ClassifiedTweet]): Future[Option[ClassificationStats]] = {
    val latestTweetsStats = calculateLatestTweetsStats(tweets)
    Firebase.get[ClassificationStats](refPath, classOf[ClassificationStats]).andThen {
      case Success(Some(stats)) => updateStatsValue(stats.combine(latestTweetsStats))
      case Success(None)        => updateStatsValue(new ClassificationStats().combine(latestTweetsStats))
    }
  }

  private def updateStatsValue(stats: ClassificationStats): Unit = {
    Firebase.save(refPath, stats, pushFirst = false)
  }

  private def calculateLatestTweetsStats(tweets: Seq[ClassifiedTweet]): ClassificationStats = {
    val totalNumberOfClassifiedTweets: Long = tweets.length
    val totalNumberOfPositiveTweets: Long   = tweets.count(_.sentiment == Sentiment.Positive.toString)
    val totalNumberOfNegativeTweets: Long   = tweets.count(_.sentiment == Sentiment.Negative.toString)
    val totalNumberOfNeutralTweets
      : Long = totalNumberOfClassifiedTweets - totalNumberOfPositiveTweets - totalNumberOfNegativeTweets
    ClassificationStats(totalNumberOfClassifiedTweets,
                        totalNumberOfPositiveTweets,
                        totalNumberOfNegativeTweets,
                        totalNumberOfNeutralTweets)
  }
}
