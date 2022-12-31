package com.andrzejressel.scalops.lambda

import izumi.reflect.macrortti.LightTypeTagRef.Refinement

import scala.annotation.tailrec
import scala.collection.immutable.{HashMap, Map}
import scala.collection.mutable
import scala.quoted.*

object CheckerMacro {

  inline def verify(inline x: Any): Unit = ${ verifyImpl('x) }

  private enum VariableError {
    case OUTSIDE_LAMBDA, OUTSIDE_LAMBDA_SERIALIZABLE
  }

  import VariableError.*

  private def verifyImpl(x: Expr[Any])(using Quotes): Expr[Unit] = {
    import quotes.reflect.*

    def findOwners(s: Symbol): List[Symbol] = s :: List.unfold(s)(s1 =>
      Option.when(s1.maybeOwner != Symbol.noSymbol)(
        (s1.maybeOwner, s1.maybeOwner)
      )
    )

    def isVariableInvalid(ident: Ident, root: Tree): Option[VariableError] = {

      val isSerializable =
        ident.symbol.typeRef <:< TypeRepr.of[java.io.Serializable]

      val owners = findOwners(ident.symbol)

      val isDefinedInObject = owners.head.companionModule == owners.head

      val definedInsideLambda = owners.contains(root.symbol)

      if !isDefinedInObject && !definedInsideLambda then
        val error =
          if isSerializable then OUTSIDE_LAMBDA_SERIALIZABLE
          else OUTSIDE_LAMBDA
        Some(error)
      else None

    }

    def unwrapInlined(t: Term): Term = {
      t match {
        case Inlined(_, _, t: Inlined) => unwrapInlined(t)
        case Inlined(_, _, t)          => t
      }
    }

    val unwrapped = unwrapInlined(x.asTerm)

    val root = unwrapped match {
      case Block(l :: _, _) => l
    }

    val lambdaBody = root match {
      case DefDef(_, _, _, Some(b)) => b
    }

    val traverser = new TreeTraverser {
      override def traverseTree(tree: Tree)(owner: Symbol): Unit = {
        tree match {
          case i @ Ident(_) =>
            val symbol = tree.symbol
            val pos    = symbol.pos.get

            isVariableInvalid(i, root) match {
              case Some(OUTSIDE_LAMBDA) =>
                report.error("Value is declared outside of lambda", i.pos)
              case Some(OUTSIDE_LAMBDA_SERIALIZABLE) =>
                report.warning(
                  "Value is declared outside of lambda, but it's serializable",
                  i.pos
                )
              case None => ()
            }
          case _ =>
        }

        super.traverseTree(tree)(root.symbol)
      }
    }

    traverser.traverseTree(lambdaBody)(lambdaBody.symbol)

    '{ () }
  }

}
