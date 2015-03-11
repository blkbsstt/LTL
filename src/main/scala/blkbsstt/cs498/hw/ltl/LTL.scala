package blkbsstt.cs498.hw.ltl

object LTL extends App {
  val problem = Problem.Boat
  val formula = problem.formula
  val parser = new LTLParser()
  parser.parse(formula) match {
    case parser.Success(result, _) => LTLSat(result, 33, 1) match {
      case Some(model) => problem.display(model)
      case None        => println("Search Failed")
    }
    case parser.NoSuccess(msg, _) => println(msg)
  }
}
