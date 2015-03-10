package blkbsstt.cs498.hw.ltl

object LTL extends App {
  val problem = Problem.SMT
  val formula = problem.formula
  val parser = new LTLParser()
  val parsed = parser.parse(formula)
  parsed match {
    case parser.Success(result, _) => LTLSat(result, 1, 3) match {
      case Some(model) => problem.display(model)
      case None        => println("Search Failed")
    }
    case parser.NoSuccess(msg, _) => println(msg)
  }
}
