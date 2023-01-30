import _root_.io.github.davidgregory084.DevMode
import _root_.com.andrzejressel.build.*
import _root_.com.andrzejressel.build.Dependencies.*

enablePlugins(DevContainerPlugin)

inScope(Global)(
  Seq(
    onChangedBuildSource := ReloadOnSourceChanges
  )
)

val currentScala = "3.2.1"
val nextScala    = "3.2.2"

inThisBuild(
  Seq(
    scalaVersion       := currentScala,
    crossScalaVersions := Seq(currentScala, nextScala),
    version            := "0.1.0-SNAPSHOT",
    organization       := "pl.andrzejressel",
    semanticdbEnabled  := true,
    semanticdbVersion  := scalafixSemanticdb.revision,
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
  )
)

ThisBuild / assemblyMergeStrategy := {
  case "META-INF/io.netty.versions.properties" => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

addCommandAlias("fix", ";scalafixAll;scalafmtAll;scalafmtSbt")
addCommandAlias(
  "check",
  ";scalafixAll --check;scalafmtCheckAll;scalafmtSbtCheck"
)

lazy val scalops = (project in file("scalops"))

lazy val root = (project in file("."))
  .aggregate(scalops)
