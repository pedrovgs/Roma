package com.github.pedrovgs.roma.storage

import com.github.pedrovgs.roma.{ClassificationSnapshotStat, ClassificationStats, ClassifiedTweet, Sentiment}
import com.google.firebase.tasks.Tasks

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

object StatsStorage {
  private val classifiedTweetsStats         = "/classifiedTweetsStats"
  private val classifiedTweetsTimelineStats = "/classifiedTweetsTimelineStats"

  def updateStats(stats: ClassificationStats): Future[Option[ClassificationStats]] = {
    updateStatsTimeline(stats)
    updateGeneralStats(stats)
  }

  def clear(): Unit = {
    Tasks.await(Firebase.remove("/classifiedTweetsStats"))
    Tasks.await(Firebase.remove("/classifiedTweetsTimelineStats"))
  }

  private def updateGeneralStats(tweetStats: ClassificationStats) = {
    Firebase.get[ClassificationStats](classifiedTweetsStats, classOf[ClassificationStats]).andThen {
      case Success(Some(stats)) => updateStatsValue(stats.combine(tweetStats))
      case Success(None)        => updateStatsValue(new ClassificationStats().combine(tweetStats))
    }
  }

  private def updateStatsTimeline(stats: ClassificationStats): Future[Option[ClassificationSnapshotStat]] = {
    val hour: Long  = System.currentTimeMillis() / (1 hour).toMillis
    val newSnapshot = ClassificationSnapshotStat(hour, stats)
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
}
