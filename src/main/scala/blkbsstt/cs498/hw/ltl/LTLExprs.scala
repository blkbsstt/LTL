package blkbsstt.cs498.hw.ltl

import scala.reflect.ClassTag

object LTLExprs {

  def props(e: LTLExpr): List[Prop] = e match {
    case p: Prop          => p :: Nil
    case NullaryExpr(_)   => Nil
    case UnaryExpr(a)     => props(a)
    case BinaryExpr(l, r) => props(l) ::: props(r)
  }

  def op(e: LTLExpr) = e match {
    case Until(l, r)   => "U"
    case Release(l, r) => "R"
    case Implies(l, r) => "→"
    case Iff(l, r)     => "↔"
    case Or(l, r)      => "∨"
    case And(l, r)     => "∧"
    case Next(e)       => "X"
    case Eventually(e) => "⋄"
    case Always(e)     => "◻"
    case Not(e)        => "¬"
    case Prop(s)       => s
    case True()        => "⊤"
    case False()       => "⊥"
  }

  def pp(e: LTLExpr): String = e match {
    case BinaryExpr(l, r) => {
      def side(a: LTLExpr) = a match {
        case _ if a.getClass == e.getClass => pp(a)
        case _: BinaryExpr[_]              => s"(${pp(a)})"
        case _                             => pp(a)
      }
      side(l) + " " + op(e) + " " + side(r)
    }
    case UnaryExpr(u) => u match {
      case BinaryExpr(_, _) => s"${op(e)}(${pp(u)})"
      case _                => s"${op(e)}${pp(u)}"
    }
    case NullaryExpr(s) => op(e)
  }

  implicit class LTLOps(e: LTLExpr) {
    val op = LTLExprs.op(e)
    def pp = LTLExprs.pp(e)
    def props = LTLExprs.props(e)
  }

  sealed trait LTLExpr

  sealed abstract class NullaryExpr(val s: String) extends LTLExpr
  object NullaryExpr {
    def unapply(g: NullaryExpr): Option[String] = Some(g.s)
  }

  sealed abstract class UnaryExpr(val e: LTLExpr) extends LTLExpr
  object UnaryExpr {
    def unapply(u: UnaryExpr): Option[LTLExpr] = Some(u.e)
  }

  sealed abstract class BinaryExpr[T <: BinaryExpr[_]: ClassTag](val l: LTLExpr, val r: LTLExpr) extends LTLExpr
  object BinaryExpr {
    def unapply(b: BinaryExpr[_]): Option[(LTLExpr, LTLExpr)] = Some(b.l, b.r)
  }

  sealed trait NormedLTLExpr extends LTLExpr
  sealed trait NormedNullaryExpr extends NullaryExpr with NormedLTLExpr
  sealed trait NormedUnaryExpr extends UnaryExpr with NormedLTLExpr
  sealed trait NormedBinaryExpr[T <: NormedBinaryExpr[_]] extends BinaryExpr[T] with NormedLTLExpr

  case class Prop(override val s: String) extends NullaryExpr(s) with NormedNullaryExpr
  case class True() extends NullaryExpr("⊤") with NormedNullaryExpr
  case class False() extends NullaryExpr("⊥") with NormedNullaryExpr
  case class Until(override val l: LTLExpr, override val r: LTLExpr) extends BinaryExpr[Until](l, r) with NormedBinaryExpr[Until]
  case class Release(override val l: LTLExpr, override val r: LTLExpr) extends BinaryExpr[Release](l, r)
  case class Implies(override val l: LTLExpr, override val r: LTLExpr) extends BinaryExpr[Implies](l, r)
  case class Iff(override val l: LTLExpr, override val r: LTLExpr) extends BinaryExpr[Iff](l, r)
  case class Or(override val l: LTLExpr, override val r: LTLExpr) extends BinaryExpr[Or](l, r) with NormedBinaryExpr[Or]
  case class And(override val l: LTLExpr, override val r: LTLExpr) extends BinaryExpr[And](l, r) with NormedBinaryExpr[And]
  case class Next(override val e: LTLExpr) extends UnaryExpr(e) with NormedUnaryExpr
  case class Eventually(override val e: LTLExpr) extends UnaryExpr(e) with NormedUnaryExpr
  case class Always(override val e: LTLExpr) extends UnaryExpr(e) with NormedUnaryExpr
  case class Not(override val e: LTLExpr) extends UnaryExpr(e) with NormedUnaryExpr
}

object NormedLTLExprs {
  import LTLExprs._

  implicit def norm(e: LTLExpr): NormedLTLExpr = e match {
    case Until(l, r)   => Until(norm(l), norm(r))
    case Release(l, r) => norm(Not(Until(Not(l), Not(r))))
    case Implies(l, r) => norm(Or(Not(l), r))
    case Iff(l, r)     => norm(And(Or(l, Not(r)), Or(Not(l), r)))
    case Or(l, r)      => Or(norm(l), norm(r))
    case And(l, r)     => And(norm(l), norm(r))
    case Next(e)       => Next(norm(e))
    case Eventually(e) => Eventually(norm(e))
    case Always(e)     => Always(norm(e))
    case Not(e)        => Not(norm(e))
    case a: Prop       => a
    case a: True       => a
    case a: False      => a
  }
}