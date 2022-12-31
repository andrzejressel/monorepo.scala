package com.andrzejressel.scalops.lambda

object Checker {

  inline def check[Z](inline f: () => Z): Unit = {
    CheckerMacro.verify(f)
  }

  inline def check[A, Z](inline f: A => Z): Unit = {
    CheckerMacro.verify(f)
  }

}
