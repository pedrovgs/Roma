package com.github.pedrovgs.roma

import org.scalatest._
import org.scalatest.Matchers._

final class RomaTest extends WordSpec with GivenWhenThen {
  "Roma" should {
    "greet" in {
      Given("a Roma")

      val roma = new Roma

      When("we ask him to greet someone")

      val nameToGreet = "CodelyTV"
      val greeting = roma.greet(nameToGreet)

      Then("it should say hello to someone")

      greeting shouldBe "Hello " + nameToGreet
    }
  }
}
