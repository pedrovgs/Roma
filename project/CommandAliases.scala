import sbt.addCommandAlias
import sbt.internals.DslEntry

object CommandAliases {

  def addCommandAliases(): DslEntry = {
    addCommandAlias("format", ";scalafmt;test:scalafmt")
  }

}
