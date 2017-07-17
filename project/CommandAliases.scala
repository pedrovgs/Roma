import sbt.addCommandAlias

object CommandAliases {

  def addCommandAliases(): sbt.internals.DslEntry = {
    addCommandAlias("format", ";scalafmt;test:scalafmt")
    addCommandAlias("c", "compile")
    addCommandAlias("t", "test")
    addCommandAlias("tc", "test:compile")
    addCommandAlias("to", "testOnly")
    addCommandAlias("tq", "testQuick")
    addCommandAlias("tsf", "testShowFailed")
  }

}
