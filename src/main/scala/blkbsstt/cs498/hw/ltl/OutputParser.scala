package blkbsstt.cs498.hw.ltl

import scala.util.parsing.combinator.PackratParsers
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader

class OutputParser extends RegexParsers with PackratParsers {
  import LTLExprs._
  import OutExprs._

  def parse(input: String) = phrase(commit(model))(new PackratReader(new CharSequenceReader(input)))

  lazy val model: PackratParser[Model] = "(model" ~> rep(valuation) ^^ { Model(_) }

  lazy val valuation: PackratParser[Valuation] =
    ("(define-fun" ~> """[<>a-zA-Z0-9!+*_]+""".r <~ "((x!1" <~ "Int)) Bool") ~ value <~ rep(")") ^^ {
      case id ~ values => Valuation(id, values.sortBy(_.position).drop(1))
    }

  lazy val value: PackratParser[List[Value]] = ("(ite (= x!1" ~> """[0-9]+""".r <~ ")") ~
    bool ~ (value | bool ^^ { List(_) }) <~ ")" ^^ {
      case i ~ x ~ v => Value(x.value, i.toInt) :: v
    }

  lazy val bool: PackratParser[Value] = ("false" | "true") ^^ { b => Value(b.toBoolean, -1) }
}

object OutExprs {
  sealed trait Result
  case class UnSat() extends Result
  case class Sat(m: Model) extends Result
  case class Model(v: List[Valuation])
  case class Valuation(id: String, v: List[Value])
  case class Value(value: Boolean, position: Int)
}
