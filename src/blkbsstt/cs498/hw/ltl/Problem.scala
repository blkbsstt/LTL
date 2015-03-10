package blkbsstt.cs498.hw.ltl

import scala.collection.TraversableOnce.MonadOps
import scala.collection.immutable.SortedSet

import blkbsstt.cs498.hw.ltl.OutExprs._
import OutExprs.Valuation

object Problem {

  trait Problem {
    def formula: String
    def display(m: OutExprs.Model) = m match {
      case OutExprs.Model(vals) => println(vals)
    }
  }

  object SMT extends Problem {
    import LTLStringOps._
    val p = "p"
    val q = "q"
    val formula = p /\ G(p -> F(q)) /\ G(q -> F(p))
  }

  object Boat extends Problem {
    import LTLStringOps._
    import OutExprs.Valuation
    val C = List(1, 2, 3)
    val b = "b"
    val w = "w"
    val m = "m"
    val l = "l"
    val r = "r"
    val Gen = SortedSet(m, w)
    val L = SortedSet(l, b, r)
    val S = L - b

    val P = for (g <- Gen; i <- C) yield g + i
    val props = (for (p <- P; k <- L) yield p + k) ++ (for (k <- S) yield b + k)

    val exists = /\(for (p <- P) yield \/(for (k <- L) yield p + k) /\ \/(for (k <- S) yield b + k))
    val distinct = /\(for (p <- P; k <- L) yield (p + k) -> /\(for (j <- L - k) yield ¬(p + j)) /\ ((b + l) -> ¬(b + r)) /\ ((b + r) -> ¬(b + l)))
    val boatcap = /\((for (p <- P) yield p + b).toSeq.combinations(3).map(/\).map(¬))
    val jealousy = /\(for (k <- L; i <- C) yield (w + i + k) -> ((m + i + k) \/ (¬(m + ((i % 3) + 1) + k) /\ ¬(m + (((i + 1) % 3) + 1) + k))))
    val driver = /\(for (l <- S) yield ->(b + l, W(b + l, \/(P.map(_ + b)))))
    val patience = /\(for (c <- S; p <- P; n <- S - c) yield ((p + c) /\ (b + n) /\ X(b + c)) -> ¬(X(p + b) \/ X(X(p + b))))
    val travel = /\(for (c <- S; p <- P; n <- S - c) yield (p + c) -> W(p + c,
      ((b + c) /\ (p + c)) /\ //the boat is here
        X((b + c) /\ (p + b)) /\ //i get on the boat
        X(X((b + n) /\ (p + b))) /\ //i go to the other side
        X(X(X((b + n) /\ (p + n)))))) //i get off the boat

    val constraints = G(/\(exists, distinct, boatcap, jealousy, driver, travel, patience))

    val fin = F(G(/\(P.map(_ + r))))
    val init = /\(P.map(_ + l) + "bl")
    val formulae = List(init, constraints, fin)

    val formula = /\(formulae)

    override def display(m: OutExprs.Model) = m match {
      case OutExprs.Model(vs) =>
        val map = Map((for (p <- props) yield (p, vs.find(_.id == p).get)).toSeq: _*)
        val bound = vs.map(_.v.size).min
        def step(i: Int, v: Valuation) = v.v.sortBy(_.position).drop(i).map(_.value) match {
          case true :: _          => v.id.take(2)
          case (false :: _) | Nil => "  "
        }
        def left(i: Int) = (for (p <- P.toSeq) yield step(i, map(p + l))).mkString("", " ", "")
        def boat(i: Int) = (if (step(i, map(b + l)) == "bl") "* " else "  ") +
          (for (p <- P.toSeq) yield step(i, map(p + b))).mkString("", " ", "") +
          (if (step(i, map(b + r)) == "br") " *" else "  ")
        def right(i: Int) = (for (p <- P.toSeq) yield step(i, map(p + r))).mkString("", " ", "")
        println((for (i <- 0 until bound) yield (s"%${(bound - 1).toString.length}d: " format i) +
          List(left(i), boat(i), right(i)).mkString("", " | ", "")).mkString("\n"))
    }
  }
}

object LTLStringOps {
  def p(e: String) = "(" + e + ")"
  def /\(l: TraversableOnce[String]) = l.map(p).mkString("""/\""")
  def \/(l: TraversableOnce[String]) = l.map(p).mkString("""\/""")
  def /\(x: String, y: String, l: String*) = (x +: (y +: l)).map(p).mkString("""/\""")
  def \/(x: String, y: String, l: String*) = (x +: (y +: l)).map(p).mkString("""\/""")
  def ->(x: String, y: String, l: String*) = (x +: (y +: l)).map(p).mkString("""->""")
  private def unary(op: String): String => String = ((e: String) => p(op + p(e)))
  val G = unary("G")
  val F = unary("F")
  val X = unary("X")
  val ¬ = unary("~")
  val neg = ¬
  def U(x: String, y: String) = s"($x)U($y)"
  def W(x: String, y: String) = \/(G(x), U(x, y))

  implicit class ImplicitStringOps(s: String) {
    def /\(o: String) = LTLStringOps./\(s, o)
    def \/(o: String) = LTLStringOps.\/(s, o)
    def ->(o: String) = LTLStringOps.->(s, o)
    def U(o: String) = LTLStringOps.U(s, o)
    def W(o: String) = LTLStringOps.W(s, o)
    def unary_~ = neg(s)
  }
}