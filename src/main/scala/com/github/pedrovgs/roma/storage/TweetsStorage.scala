package com.github.pedrovgs.roma.storage

import com.github.pedrovgs.roma.ClassifiedTweet

import scala.concurrent.Future

class TweetsStorage(val firebase: Firebase) {

  def saveTweets(tweets: Seq[ClassifiedTweet]): Future[Seq[ClassifiedTweet]] = {
    firebase.save("/classifiedTweets", tweets)
  }

}
