name := "Roma"
version := "0.0.1"
scalaVersion := "2.11.11"
assemblyJarName in assembly := "roma.jar"
mainClass in assembly := Some("com.github.pedrovgs.roma.RomaApplication")

enablePlugins(ScalafmtPlugin)
CommandAliases.addCommandAliases()

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % Versions.spark % Provided,
  "org.apache.spark" %% "spark-streaming" % Versions.spark % Provided,
  "org.apache.spark" %% "spark-sql" % Versions.spark % Provided,
  "org.apache.spark" %% "spark-mllib" % Versions.spark % Provided,
  "com.lihaoyi" %% "pprint" % Versions.pprint,
  "org.apache.bahir" %% "spark-streaming-twitter" % Versions.sparkStreamingTwitter,
  "com.typesafe" % "config" % Versions.config,
  "com.google.firebase" % "firebase-server-sdk" % Versions.firebase,
  "com.vdurmont" % "emoji-java" % Versions.emojiJava

)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % Versions.scalaMock % Test,
  "org.scalacheck" %% "scalacheck" % Versions.scalaCheck % Test,
  "com.holdenkarau" %% "spark-testing-base" % Versions.sparkTestingBase % Test
)

import sbtassembly.MergeStrategy

assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", "spark", "unused", "UnusedStubClass.class") => MergeStrategy.first
  case PathList("META-INF", _) => MergeStrategy.discard
  case _ => MergeStrategy.first
}


test in assembly := {}
fork in Test := true
parallelExecution in Test := false
javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled")

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
