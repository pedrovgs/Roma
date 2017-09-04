package com.github.pedrovgs.roma

import com.github.pedrovgs.roma.model._

import scala.beans.BeanProperty

object model {
  type Content = String
  type Score = Double
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
