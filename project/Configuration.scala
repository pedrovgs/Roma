import sbt.{Tests, _}
import sbt.Keys._

object Configuration {
  val settings = Seq(
    organization := "com.github.pedrovgs",
    scalaVersion := "2.12.2",
    // Compiler options
    scalacOptions ++= Seq(
      "-deprecation", // Warnings deprecation
      "-feature", // Advise features
      "-unchecked", // More warnings. Strict
      "-Xlint", // More warnings when compiling
      "-Xfatal-warnings", // Warnings became errors
      "-Ywarn-dead-code",
      "-Ywarn-unused",
      "-Ywarn-unused-import",
      "-Xcheckinit" // Check against early initialization
    ),
    scalacOptions in run in Compile -= "-Xcheckinit", // Remove it in production because it's expensive
    javaOptions += "-Duser.timezone=UTC",
    // Test options
    parallelExecution in Test := false,
    testForkedParallel in Test := false,
    fork in Test := true,
    testOptions in Test ++= Seq(
      Tests.Argument(TestFrameworks.ScalaTest, "-u", "target/test-reports"), // Save test reports
      Tests.Argument("-oDF") // Show full stack traces and time spent in each test
    )
  )
}
