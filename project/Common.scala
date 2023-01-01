package com.andrzejressel.build

import sbt._
import _root_.io.github.davidgregory084.TpolecatPlugin.autoImport._

object Common {
  val settings = Seq(
    tpolecatScalacOptions ++= Set(
      ScalacOptions.source3,
      ScalacOptions.languageImplicitConversions,
      ScalacOptions.sourceFuture
    )
  )
}
