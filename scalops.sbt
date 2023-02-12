import _root_.io.github.davidgregory084.DevMode
import _root_.com.andrzejressel.build.*
import _root_.com.andrzejressel.build.Dependencies.*

//ThisBuild / tpolecatDefaultOptionsMode := DevMode

//noinspection SbtDependencyVersionInspection
//scala-steward is taking care of that
lazy val scalopsCommonSettings = Common.settings ++ Seq(
  libraryDependencies ++= Seq(
    "org.scala-sbt.ipcsocket" % "ipcsocket"     % "1.6.2",
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
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys    := Seq[BuildInfoKey](version, scalaVersion),
    buildInfoPackage := "com.andrzejressel.scalops.common"
  )

lazy val scalopsClient = (project in file("client"))
  .dependsOn(scalopsCommon)
  .enablePlugins(JavaAppPackaging)
  .settings(scalopsCommonSettings)

lazy val scalopsIpcCore = (project in file("ipc/core"))
  .dependsOn(scalopsLambdaChecker)
  .settings(scalopsCommonSettings)
  .settings(
    libraryDependencies ++= ZIOSchema
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
  .configs(IntegrationTest)
  .settings(scalopsCommonSettings)
  .dependsOn(scalopsCommon)
  .dependsOn(scalopsServer)
  .settings(
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test"     % zioVersion % IntegrationTest,
      "dev.zio" %% "zio-test-sbt" % zioVersion % IntegrationTest
    ),
    (IntegrationTest / test) := ((IntegrationTest / test) dependsOn (scalopsClient / assembly)).value
  )

val scalopsProjects = Seq(
  scalopsCommon,
  scalopsClient,
  scalopsIpcCore,
  scalopsIpcLambda,
  scalopsServer,
  scalopsLambdaChecker
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
