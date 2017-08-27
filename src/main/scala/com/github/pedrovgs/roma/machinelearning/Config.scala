package com.github.pedrovgs.roma.machinelearning

private[machinelearning] object Config {
  val numberOfIterations: Int = 100
  val relevantWrods = "relevant"
  val tweetWordsColumnName = "words"
  val tweetContentColumnName = "tweetContent"
  val rawFeaturesColumnName = "rawFeatures"
  val featuresColumnName = "features"
  val labelColumnName = "label"
}
