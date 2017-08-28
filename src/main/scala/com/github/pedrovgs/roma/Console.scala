package com.github.pedrovgs.roma

object Console {

  def print(any: Any): Unit = {
    println(scala.Console.CYAN + any + scala.Console.RESET)
  }

  def smallSeparator(): Unit = {
    separator(20)
  }

  def separator(numberOfSeparators: Int = 60): Unit = {
    val separator = Array.fill(numberOfSeparators)("-").mkString
    print(separator)
  }

}
