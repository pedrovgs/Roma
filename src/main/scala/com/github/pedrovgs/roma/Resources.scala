package com.github.pedrovgs.roma

import java.io.File
import java.nio.file.{Files, Paths}

import org.apache.commons.io.FileUtils
import org.apache.spark.SparkFiles

trait Resources {
  def getFilePath(name: String): String = {
    val jarPath    = getClass.getResource(name).getPath
    val volumePath = "/tmp/data/resources" + name
    if (exists(jarPath)) {
      pprint.pprintln("Reading existing jar path at: " + jarPath)
      jarPath
    } else if (exists(volumePath)) {
      pprint.pprintln("Reading existing volume path at: " + volumePath)
      "file://" + volumePath + "/"
    } else {
      val fileName   = name.substring(name.lastIndexOf("/") + 1)
      val workerPath = SparkFiles.get(fileName)
      if (exists(workerPath)) {
        pprint.pprintln("Reading existing worker path at: " + workerPath)
      } else {
        pprint.pprintln("Couln't find file: " + fileName)
      }
      "file://" + workerPath + "/"
    }
  }

  def getOutputFilePath(name: String): String = "./outputs/" + name

  def delete(path: String): Unit = {
    FileUtils.deleteDirectory(new File(path))
  }

  private def exists(path: String): Boolean = Files.exists(Paths.get(path))
}
