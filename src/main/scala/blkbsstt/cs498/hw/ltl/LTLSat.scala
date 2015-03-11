package blkbsstt.cs498.hw.ltl

import java.io.ByteArrayInputStream
import java.lang.Math.min

import scala.sys.process.stringToProcess

import LTLExprs.{Always, And, Eventually, False, LTLExpr, LTLOps, Next, NormedLTLExpr, Not, Or, Prop, True, Until}
import NormedLTLExprs.norm
import OutExprs.Model

object LTLSat {
  import LTLtoZ3._
  import NormedLTLExprs.norm
  import OutExprs._

  def apply(e: LTLExprs.LTLExpr, r: Int = 20, s: Int = 20, trySearch: Boolean = false) = {
    val (rb, sb) = if (trySearch) (1, 0) else (r, s)
    search(e, rb, sb, r, s)
  }

  def nextpair(r: Int, s: Int): (Int, Int) = if (r == 0) (s + 1, 0) else (r - 1, s + 1)

  def search(e: LTLExprs.LTLExpr, r: Int, s: Int, rlim: Int, slim: Int): Option[Model] =
    if (r <= rlim && s <= slim) {
      println(s"Satisfiable with bound: ${(r, s)}?")
      val z3encoding = LTLtoZ3(e, r, s).mkString("\n")
      println("Done encoding, passing to z3")
      val input = new ByteArrayInputStream(z3encoding.getBytes)
      val z3out = ("z3 -smt2 -in" #< input).lines_!
      println("Done with z3, extracting model from output")
      val isSat :: model = z3out.mkString("\n").split("\n").toList
      if (isSat == "sat") {
        println("Formula is satisfiable within current bounds")
        extractmodel(model)
      } else {
        println("Formula is unsatisfiable with current bounds")
        val (rn, sn) = nextpair(r, s)
        search(e, rn, sn, rlim, slim)
      }
    } else None

  def extractmodel(model: Seq[String]): Option[Model] = {
    val parser = new OutputParser()
    parser.parse(model.mkString("\n")) match {
      case parser.Success(res, _)   => Some(res)
      case parser.NoSuccess(msg, _) => println(msg); None
    }
  }
}


object LTLtoZ3 {
  import LTLExprs._
  import NormedLTLExprs.norm

  def apply(e: LTLExpr, r: Int, s: Int): List[String] = {
    val props = e.props.distinct
    val declares = for (p <- props) yield declare(p)
    val assertion = assert(e, r, s)
    val ending = List("(check-sat)", "(get-model)", "(exit)")
    declares ::: assertion :: ending
  }

  def declare(p: Prop) = s"(declare-fun ${p.s} (Int) Bool)"

  def assert(e: NormedLTLExpr, r: Int, s: Int): String = s"(assert ${expand(e, 0, r, s)})"

  object Z3StringOps {
    def /\(l: TraversableOnce[String]) = if (l.isEmpty) "" else l.mkString("(and ", " ", ")")
    def \/(l: TraversableOnce[String]) = if (l.isEmpty) "" else l.mkString("(or ", " ", ")")
    def /\(x: String, y: String, l: String*) = (x +: (y +: l)).mkString("(and ", " ", ")")
    def \/(x: String, y: String, l: String*) = (x +: (y +: l)).mkString("(or ", " ", ")")
    def not(x: String) = s"(not $x)"
  }

  def expand(e: NormedLTLExpr, i: Int, r: Int, s: Int): String =
    {
      import Z3StringOps._
      import Math.min
      val l = r
      val k = r + s - 1
      def succ(i: Int) = if (i < k) i + 1 else l
      e match {
        case Or(f, g)  => \/(expand(f, i, r, s), expand(g, i, r, s))
        case And(f, g) => /\(expand(f, i, r, s), expand(g, i, r, s))
        case Not(f)    => not(expand(f, i, r, s))
        case False()   => "false"
        case True()    => "true"
        case p: Prop   => s"(${p.s} $i)"
        case Next(f)   => expand(f, succ(i), r, s)

        case Until(f, g) =>
          \/(
            \/(for (j <- i to k) yield /\(
              expand(g, j, r, s),
              /\(for (n <- i to j - 1) yield expand(f, n, r, s)))),
            \/(for (j <- l to i - 1) yield /\(
              expand(g, j, r, s),
              /\(for (n <- i to k) yield expand(f, n, r, s)),
              /\(for (n <- l to j - 1) yield expand(f, n, r, s)))))
        case Always(f)     => /\(for (j <- min(i, l) to k) yield expand(f, j, r, s))
        case Eventually(f) => \/(for (j <- min(i, l) to k) yield expand(f, j, r, s))
      }
    }
}
