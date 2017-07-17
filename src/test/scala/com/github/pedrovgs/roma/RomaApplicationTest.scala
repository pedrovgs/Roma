package com.github.pedrovgs.roma

import com.holdenkarau.spark.testing.SharedSparkContext
import org.scalatest._

final class RomaApplicationTest extends FlatSpec with Matchers with SharedSparkContext {

  "RomaApplication" should "execute this dummy test" in {
    true shouldBe true
  }

}
