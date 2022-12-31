import _root_.io.github.davidgregory084.DevMode

enablePlugins(DevContainerPlugin)

inScope(Global)(
  Seq(
    onChangedBuildSource := ReloadOnSourceChanges
  )
)

inThisBuild(
  Seq(
    scalaVersion      := "3.2.1",
    version           := "0.1.0-SNAPSHOT",
    organization      := "pl.andrzejressel.scalops",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
  )
)

ThisBuild / assemblyMergeStrategy := {
  case "META-INF/io.netty.versions.properties" => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

//ThisBuild / tpolecatDefaultOptionsMode := DevMode

val zioVersion       = "2.0.5"
val zioSchemaVersion = "0.4.1"
val scodecVersion    = "2.1.0"

//noinspection SbtDependencyVersionInspection
//scala-steward is taking care of that
lazy val commonSettings = Seq(
  tpolecatScalacOptions ++= Set(
    ScalacOptions.source3,
    ScalacOptions.languageImplicitConversions,
    ScalacOptions.sourceFuture
  ),
  libraryDependencies ++= Seq(
    "dev.zio"                %% "zio"            % zioVersion,
    "dev.zio"                %% "zio-streams"    % zioVersion,
    "dev.zio"                %% "zio-concurrent" % zioVersion,
    "org.scala-sbt.ipcsocket" % "ipcsocket"      % "1.4.0",
    "org.apache.commons"      % "commons-lang3"  % "3.12.0",
    "commons-io"              % "commons-io"     % "2.11.0"   % Test,
    "dev.zio"                %% "zio-test"       % zioVersion % Test,
    "dev.zio"                %% "zio-test-sbt"   % zioVersion % Test,
    "org.scodec"             %% "scodec-core"    % scodecVersion,
    "dev.zio"                %% "zio-process"    % "0.7.1"
  ),
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
)

lazy val common = (project in file("common"))
  .dependsOn(ipcCore, ipcLambda)
  .settings(commonSettings)

lazy val client = (project in file("client"))
  .dependsOn(common)
  .enablePlugins(JavaAppPackaging)
  .settings(commonSettings)

lazy val ipcCore = (project in file("ipc/core"))
  .dependsOn(lambdaChecker)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-schema"            % zioSchemaVersion,
      "dev.zio" %% "zio-schema-derivation" % zioSchemaVersion,
      "dev.zio" %% "zio-schema-json"       % zioSchemaVersion,
      "dev.zio" %% "zio-schema-msg-pack"   % zioSchemaVersion
    )
  )

lazy val ipcLambda = (project in file("ipc/lambda"))
  .dependsOn(ipcCore, lambdaChecker)
  .settings(commonSettings)

lazy val server = (project in file("server"))
  .dependsOn(common)
  .settings(commonSettings)

lazy val lambdaChecker = (project in file("lambda_checker"))
  .settings(commonSettings)

lazy val integrationTests = (project in file("integration_tests"))
  .settings(commonSettings)
  .dependsOn(common)
  .dependsOn(server)
  .settings(
    (Test / test) := ((Test / test) dependsOn (client / assembly)).value
  )

lazy val root = (project in file("."))
  .aggregate(
    server,
    common,
    client,
    ipcCore,
    ipcLambda,
    lambdaChecker
  )
  .settings(
    name := "scalops"
  )

addCommandAlias("fix", ";scalafixAll;scalafmtAll;scalafmtSbt")
addCommandAlias(
  "check",
  ";scalafixAll --check;scalafmtCheckAll;scalafmtSbtCheck"
)
addCommandAlias(
  "allTest",
  s";test;${integrationTests.id}/test"
)
