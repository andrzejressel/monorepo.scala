package com.andrzejressel.scalops.lambda

import zio.Scope
import zio.test.*

import scala.compiletime.testing.ErrorKind.Typer
import scala.compiletime.testing.{Error as TError, typeCheckErrors}

object CheckerSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CheckerSpec")(
      test("Should work on empty lambda") {
        val r = """
          Checker.check(() => "123")
        """

        val errors = typeCheckErrors(r)

        assertTrue(errors.isEmpty)
      },
      test("Should work with variable declared inside lambda") {
        val r = """
          Checker.check((a: String) =>
            val a = ""
            a
          )
        """

        val errors = typeCheckErrors(r)

        assertTrue(errors.isEmpty)
      },
      test("Should return error when variable is declared before lambda") {
        val r = """
          class A {}
          val p = A()
          Checker.check(() => p.toString())
        """

        val errors = typeCheckErrors(r)

        assertTrue(
          errors == List(
            TError(
              "Value is declared outside of lambda",
              "          Checker.check(() => p.toString())",
              30,
              Typer
            )
          )
        )
      },
      test(
        "Should return warning when variable declared before lambda is serialized"
      ) {
        val r = """
          class A extends Serializable {}
          val p = A()
          Checker.check(() => p.toString())
        """

        val errors = typeCheckErrors(r)

        assertTrue(
          errors == List(
            TError(
              "Value is declared outside of lambda, but it's serializable",
              "          Checker.check(() => p.toString())",
              30,
              Typer
            )
          )
        )
      },
      test("Should work when external variable es object field") {
        val r = """
          object A {
            val a = "abc"
          }
          Checker.check(() => A.a + "abc")
        """

        val errors = typeCheckErrors(r)

        assertTrue(errors.isEmpty)
      }
    )
}
