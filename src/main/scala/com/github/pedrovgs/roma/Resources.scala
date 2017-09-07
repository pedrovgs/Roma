package com.github.pedrovgs.roma

import com.github.pedrovgs.roma.Console._
import java.io.File
import java.nio.file.{Files, Paths}

import org.apache.commons.io.FileUtils
import org.apache.spark.SparkFiles

trait Resources {
  def getFilePath(name: String): String = {
    val jarPath    = getClass.getResource(name)
    val volumePath = "/tmp/data/resources" + name
    if (jarPath != null && exists(jarPath.getPath)) {
      print("Reading existing jar path at: " + jarPath.getPath)
      jarPath.getPath
    } else if (exists(volumePath)) {
      print("Reading existing volume path at: " + volumePath)
      "file://" + volumePath + "/"
    } else {
      ""
    }
  }

  def getOutputFilePath(name: String): String = "./outputs/" + name

  def delete(path: String): Unit = {
    FileUtils.deleteDirectory(new File(path))
  }

  private def exists(path: String): Boolean = {
    val file = new File(path)
    file.exists() || file.isDirectory
  }
}
