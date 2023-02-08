package com.andrzejressel.build

import sbt.Keys.scalaVersion
import sbt.{librarymanagement, *}

object Dependencies {

  val zioVersion        = "2.0.7"
  val zioSchemaVersion  = "0.4.7"
  val zioProcessVersion = "0.7.1"
  val scodecVersion     = "2.2.0"

  val ZIO = {
    val core         = "dev.zio" %% "zio"               % zioVersion
    val streams      = "dev.zio" %% "zio-streams"       % zioVersion
    val concurrent   = "dev.zio" %% "zio-concurrent"    % zioVersion
    val process      = "dev.zio" %% "zio-process"       % zioProcessVersion
    val test         = "dev.zio" %% "zio-test"          % zioVersion % Test
    val testSbt      = "dev.zio" %% "zio-test-sbt"      % zioVersion % Test
    val testMagnolia = "dev.zio" %% "zio-test-magnolia" % zioVersion % Test

    Seq(core, streams, concurrent, process, test, testSbt, testMagnolia)
  }

  val Scodec = {
    val core = "org.scodec" %% "scodec-core" % scodecVersion
    Seq(core)
  }

}
