package com.github.pedrovgs.roma

import com.github.pedrovgs.roma.model.{Confidence, Content}

object model {
  type Content = String
  type Confidence = Double
}

sealed trait Sentiment
case object Love
case object Hate

case class ClassifiedTweet(content: Content, sentiment: Sentiment, confidence: Confidence)

