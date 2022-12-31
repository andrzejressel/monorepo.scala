package com.andrzejressel.scalops.server.handler

import com.andrzejressel.scalops.ipc.core.Connection
import zio.Scope
import zio.test.*

object GetClassHandlerSpec extends ZIOSpecDefault {

  def spec: Spec[TestEnvironment & Scope, Any] = suite("GetClassHandlerSpec")(
    test("Should return Some for existing class") {
      for {
        clz <- GetClassHandler(classOf[Connection].getName)
      } yield assertTrue(clz.is(_.some).size > 10)
    },
    test("Should return None for nonexisting class") {
      for {
        clz <- GetClassHandler("non.existing.class")
      } yield assertTrue(clz.isEmpty)
    }
  )
}
