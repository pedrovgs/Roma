package com.github.pedrovgs.roma

import com.github.pedrovgs.roma.model.{Confidence, Content}

import scala.beans.BeanProperty

object model {
  type Content = String
  type Confidence = Double
}

sealed trait Sentiment

case object Love extends Sentiment

case object Hate extends Sentiment

class FirebaseError extends Throwable

class ClassifiedTweet(@BeanProperty var content: Content, @BeanProperty var sentiment: Sentiment, @BeanProperty var confidence: Confidence) {

  def this() = this("", null, 0)

}
