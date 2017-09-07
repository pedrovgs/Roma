package com.github.pedrovgs.roma.storage

import com.github.pedrovgs.roma.ClassifiedTweet
import com.google.firebase.tasks.Tasks

import scala.concurrent.Future

object TweetsStorage {

  def saveTweets(tweets: Seq[ClassifiedTweet]): Future[Seq[ClassifiedTweet]] =
    Firebase.save("/classifiedTweets", values = tweets)

  def clear(): Unit = {
    Tasks.await(Firebase.remove("/classifiedTweets"))
  }
}
