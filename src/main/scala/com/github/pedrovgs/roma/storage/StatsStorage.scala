package com.github.pedrovgs.roma.storage

import com.github.pedrovgs.roma.{ClassificationSnapshotStat, ClassificationStats, ClassifiedTweet, Sentiment}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

object StatsStorage {
  private val classifiedTweetsStats         = "/classifiedTweetsStats"
  private val classifiedTweetsTimelineStats = "/classifiedTweetsTimelineStats"

  def updateStats(tweets: Seq[ClassifiedTweet]): Future[Option[ClassificationStats]] = {
    updateStatsTimeline(tweets)
    updateGeneralStats(tweets)
  }

  private def updateGeneralStats(tweets: Seq[ClassifiedTweet]) = {
    val latestTweetsStats = calculateLatestTweetsStats(tweets)
    Firebase.get[ClassificationStats](classifiedTweetsStats, classOf[ClassificationStats]).andThen {
      case Success(Some(stats)) => updateStatsValue(stats.combine(latestTweetsStats))
      case Success(None)        => updateStatsValue(new ClassificationStats().combine(latestTweetsStats))
    }
  }

  private def updateStatsTimeline(tweets: Seq[ClassifiedTweet]): Future[Option[ClassificationSnapshotStat]] = {
    val latestTweetsStats = calculateLatestTweetsStats(tweets)
    val hour: Long        = System.currentTimeMillis() / (1 hour).toMillis
    val newSnapshot       = ClassificationSnapshotStat(hour, latestTweetsStats)
    Firebase
      .get[ClassificationSnapshotStat](snapshotFirebasePath(newSnapshot), classOf[ClassificationSnapshotStat])
      .andThen {
        case Success(Some(snapshot)) =>
          updateStatsValue(snapshot.combine(newSnapshot))
        case Success(None) =>
          updateStatsValue(new ClassificationSnapshotStat(hour).combine(newSnapshot))
      }
  }

  private def updateStatsValue(stats: ClassificationStats): Unit =
    Firebase.save(classifiedTweetsStats, stats, pushFirst = false)

  private def updateStatsValue(snapshot: ClassificationSnapshotStat): Unit =
    Firebase.save(snapshotFirebasePath(snapshot), snapshot, pushFirst = false)

  private def snapshotFirebasePath(snapshot: ClassificationSnapshotStat): String = {
    val hour = snapshot.hour
    classifiedTweetsTimelineStats + s"/$hour"
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
