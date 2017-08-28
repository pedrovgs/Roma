package com.github.pedrovgs.roma

import com.github.pedrovgs.roma.model._

import scala.beans.BeanProperty

object model {
  type Content    = String
  type Score = Double
}

class FirebaseError extends Throwable

case class ClassifiedTweet(@BeanProperty var content: Content,
                           @BeanProperty var loveTweet: Boolean,
                           @BeanProperty var score: Score) {

  def this() = this("", false, 0)

}
