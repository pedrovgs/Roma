package com.github.pedrovgs.roma.extensions

object StringExtension {

  class RichString(string: String) {
    def removeConsecutiveChars(): String = {
      val str = string
      if (str == null) {
        return null
      }
      val strLen = str.length
      if (strLen <= 1) {
        return str
      }
      val strChar       = str.toCharArray
      var temp          = strChar(0)
      val stringBuilder = new StringBuilder(strLen)
      var i             = 1
      while (i < strLen) {
        val value = strChar(i)
        if (value != temp) {
          stringBuilder.append(temp)
          temp = value
        }
        i += 1
        i - 1
      }
      stringBuilder.append(temp)
      stringBuilder.toString
    }
  }

  implicit def richString(string: String): RichString = new RichString(string)

}
