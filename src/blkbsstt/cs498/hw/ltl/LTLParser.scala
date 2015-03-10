package blkbsstt.cs498.hw.ltl

import scala.util.parsing.combinator.PackratParsers
import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.CharSequenceReader

class LTLParser extends JavaTokenParsers with PackratParsers {
  import LTLExprs._

  def parse(input: String) = phrase(ltlexpr)(new PackratReader(new CharSequenceReader(input)))

  lazy val ltlexpr: PackratParser[LTLExpr] = until | err("ack!")

  lazy val until: PackratParser[LTLExpr] = prefix ~ (("U" | "R") ~ until) ^^ {
    case l ~ ("U" ~ r) => Until(l, r)
    case l ~ ("R" ~ r) => Release(l, r)
  } | prefix

  lazy val prefix: PackratParser[LTLExpr] = ("X" | "G" | "F") ~ prefix ^^ {
    case "X" ~ l => Next(l)
    case "G" ~ l => Always(l)
    case "F" ~ l => Eventually(l)
  } | impl

  lazy val impl:    PackratParser[LTLExpr] = iff ~ ("->" ~> impl) ^^ { case l ~ r => Implies(l, r) } | iff
  lazy val iff:     PackratParser[LTLExpr] = or ~ ("<->" ~> iff) ^^ { case l ~ r => Iff(l, r) } | or
  lazy val or:      PackratParser[LTLExpr] = and ~ ("""\/""" ~> or) ^^ { case l ~ r => Or(l, r) } | and
  lazy val and:     PackratParser[LTLExpr] = not ~ ("""/\""" ~> and) ^^ { case l ~ r => And(l, r) } | not
  lazy val not:     PackratParser[LTLExpr] = "~" ~> not ^^ Not | primary
  lazy val primary: PackratParser[LTLExpr] = "(" ~> primary <~ ")" | "(" ~> ltlexpr <~ ")" | prop
  lazy val prop:    PackratParser[LTLExpr] = """[a-z0-9]+""".r ^^ Prop
}
