package com.github.pedrovgs.roma.storage

import com.github.pedrovgs.roma.ClassifiedTweet

import scala.concurrent.Future

object TweetsStorage {

  def saveTweets(tweets: Seq[ClassifiedTweet]): Future[Seq[ClassifiedTweet]] =
    Firebase.save("/classifiedTweets", values = tweets)

}
