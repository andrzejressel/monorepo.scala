package com.andrzejressel.build

import sbt.Keys.scalaVersion
import sbt.{librarymanagement, *}

object Dependencies {

  val zioVersion        = "2.0.10"
  val zioSchemaVersion  = "0.4.8"
  val zioProcessVersion = "0.7.2"
  val scodecVersion     = "2.2.1"

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

  val ZIOSchema = {
    val core       = "dev.zio" %% "zio-schema"            % zioSchemaVersion
    val derivation = "dev.zio" %% "zio-schema-derivation" % zioSchemaVersion
    val json       = "dev.zio" %% "zio-schema-json"       % zioSchemaVersion
    val msgpack    = "dev.zio" %% "zio-schema-msg-pack"   % zioSchemaVersion

    Seq(core, derivation, json, msgpack)
  }

  val Scodec = {
    val core = "org.scodec" %% "scodec-core" % scodecVersion
    Seq(core)
  }

}
