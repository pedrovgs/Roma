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

case class ClassificationStats(@BeanProperty var numberOfClassifiedTweets: Long,
                               @BeanProperty var numberOfPositiveTweets: Long,
                               @BeanProperty var numberOfNegativeTweets: Long,
                               @BeanProperty var numberOfNeutralTweets: Long) {

  def this() = this(0, 0, 0, 0)

  def combine(classificationStats: ClassificationStats): ClassificationStats = {
    val newTotalTweets    = numberOfClassifiedTweets + classificationStats.numberOfClassifiedTweets
    val newPositiveTweets = numberOfPositiveTweets + classificationStats.numberOfPositiveTweets
    val negativeTweets    = numberOfNegativeTweets + classificationStats.numberOfNegativeTweets
    val neutralTweets     = numberOfNeutralTweets + classificationStats.numberOfNeutralTweets
    ClassificationStats(newTotalTweets, newPositiveTweets, negativeTweets, neutralTweets)
  }
}

case class ClassificationSnapshotStat(@BeanProperty var hour: Long, @BeanProperty var stats: ClassificationStats) {
  def this() = this(0, new ClassificationStats())
  def this(hour: Long) = this(hour, new ClassificationStats())

  def combine(snapshot: ClassificationSnapshotStat): ClassificationSnapshotStat =
    ClassificationSnapshotStat(hour, stats.combine(snapshot.stats))
}
