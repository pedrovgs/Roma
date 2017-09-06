package com.github.pedrovgs.roma

import com.github.pedrovgs.roma.model._

import scala.beans.BeanProperty

object model {
  type Content = String
  type Score   = Double
}

class FirebaseError extends Throwable

object Sentiment extends Enumeration {
  type Sentiment = Value
  val Positive, Negative, Neutral = Value
}

case class ClassifiedTweet(@BeanProperty var content: Content,
                           @BeanProperty var sentiment: String,
                           @BeanProperty var score: Score) {

  def this() = this("", Sentiment.Neutral.toString, 0)

}

case class ClassificationStats(@BeanProperty var totalNumberOfClassifiedTweets: Long,
                               @BeanProperty var totalNumberOfPositiveTweets: Long,
                               @BeanProperty var totalNumberOfNegativeTweets: Long,
                               @BeanProperty var totalNumberOfNeutralTweets: Long) {

  def this() = this(0, 0, 0, 0)

  def combine(classificationStats: ClassificationStats): ClassificationStats = {
    val newTotalTweets    = totalNumberOfClassifiedTweets + classificationStats.totalNumberOfClassifiedTweets
    val newPositiveTweets = totalNumberOfPositiveTweets + classificationStats.totalNumberOfPositiveTweets
    val negativeTweets    = totalNumberOfNegativeTweets + classificationStats.totalNumberOfNegativeTweets
    val neutralTweets     = totalNumberOfNeutralTweets + classificationStats.totalNumberOfNeutralTweets
    ClassificationStats(newTotalTweets, newPositiveTweets, negativeTweets, neutralTweets)
  }
}

case class ClassificationSnapshotStat(@BeanProperty var hour: Long, @BeanProperty var stats: ClassificationStats) {
  def this() = this(0, new ClassificationStats())
  def this(hour: Long) = this(hour, new ClassificationStats())

  def combine(snapshot: ClassificationSnapshotStat): ClassificationSnapshotStat =
    ClassificationSnapshotStat(hour, stats.combine(snapshot.stats))
}
