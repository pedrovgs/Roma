package com.github.pedrovgs.roma

import com.github.pedrovgs.roma.model.{Confidence, Content}

import scala.beans.BeanProperty

object model {
  type Content = String
  type Confidence = Double
}

class FirebaseError extends Throwable

case class ClassifiedTweet(@BeanProperty var content: Content, @BeanProperty var loveTweet: Boolean, @BeanProperty var confidence: Confidence) {

  def this() = this("", false, 0)

}
