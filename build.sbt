name := "Roma"
version := "0.0.1"
scalaVersion := "2.11.11"
assemblyJarName in assembly := "roma.jar"
mainClass in assembly := Some("com.github.pedrovgs.roma.RomaApplication")

enablePlugins(ScalafmtPlugin)
CommandAliases.addCommandAliases()

libraryDependencies ++=  Seq(
  "org.apache.spark" %% "spark-core" % Versions.spark % Provided,
  "org.apache.spark" %% "spark-streaming" % Versions.spark % Provided,
  "org.apache.spark" %% "spark-sql" % Versions.spark % Provided,
  "org.apache.spark" %% "spark-mllib" % Versions.spark % Provided,
  "com.lihaoyi" %% "pprint" % Versions.pprint
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % Versions.scalaMock % Test,
  "org.scalacheck" %% "scalacheck" % Versions.scalaCheck % Test,
  "com.holdenkarau" %% "spark-testing-base" % Versions.sparkTestingBase % Test
)

