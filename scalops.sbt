import _root_.io.github.davidgregory084.DevMode
import _root_.com.andrzejressel.build.*
import _root_.com.andrzejressel.build.Dependencies.*

//ThisBuild / tpolecatDefaultOptionsMode := DevMode

//noinspection SbtDependencyVersionInspection
//scala-steward is taking care of that
lazy val scalopsCommonSettings = Common.settings ++ Seq(
  libraryDependencies ++= Seq(
    "org.scala-sbt.ipcsocket" % "ipcsocket"     % "1.6.1",
    "org.apache.commons"      % "commons-lang3" % "3.12.0",
    "commons-io"              % "commons-io"    % "2.11.0" % Test
  ),
  libraryDependencies ++= ZIO,
  libraryDependencies ++= Scodec,
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
)

lazy val zioCommonSettings = scalopsCommonSettings ++ Seq(
)

lazy val scalopsCommon = (project in file("common"))
  .dependsOn(scalopsIpcCore, scalopsIpcLambda)
  .settings(scalopsCommonSettings)

lazy val scalopsClient = (project in file("client"))
  .dependsOn(scalopsCommon)
  .enablePlugins(JavaAppPackaging)
  .settings(scalopsCommonSettings)

lazy val scalopsIpcCore = (project in file("ipc/core"))
  .dependsOn(scalopsLambdaChecker)
  .settings(scalopsCommonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-schema"            % zioSchemaVersion,
      "dev.zio" %% "zio-schema-derivation" % zioSchemaVersion,
      "dev.zio" %% "zio-schema-json"       % zioSchemaVersion,
      "dev.zio" %% "zio-schema-msg-pack"   % zioSchemaVersion
    )
  )

lazy val scalopsIpcLambda = (project in file("ipc/lambda"))
  .dependsOn(scalopsIpcCore, scalopsLambdaChecker)
  .settings(scalopsCommonSettings)

lazy val scalopsServer = (project in file("server"))
  .dependsOn(scalopsCommon)
  .settings(scalopsCommonSettings)

lazy val scalopsLambdaChecker = (project in file("lambda_checker"))
  .settings(scalopsCommonSettings)

lazy val scalopsIntegrationTests = (project in file("integration_tests"))
  .settings(scalopsCommonSettings)
  .dependsOn(scalopsCommon)
  .dependsOn(scalopsServer)
  .settings(
    (Test / test) := ((Test / test) dependsOn (scalopsClient / assembly)).value
  )

val scalopsProjects = Seq(
  scalopsCommon,
  scalopsClient,
  scalopsIpcCore,
  scalopsIpcLambda,
  scalopsServer,
  scalopsLambdaChecker
)

addCommandAlias(
  "scalopsTest",
  scalopsProjects.map(p => s";${p.id}/test").mkString
)

addCommandAlias(
  "scalopsIntegrationTest",
  s";${scalopsIntegrationTests.id}/test"
)

lazy val scalops = (project in file("scalops"))
  .aggregate(
    scalopsServer,
    scalopsCommon,
    scalopsClient,
    scalopsIpcCore,
    scalopsIpcLambda,
    scalopsLambdaChecker,
    scalopsIntegrationTests
  )
  .settings(
    name := "scalops"
  )
