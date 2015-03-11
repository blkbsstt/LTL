LTL Model Checker
=================

This is a Scala library for specifying and model checking formulas in [Linear Temporal Logic](http://en.wikipedia.org/wiki/Linear_temporal_logic).

There is a DSL defined in Problem.scala that allows for a mildly succinct representation of complex LTL formulas.
You can find two example problems in Problem.scala as well (one quite simple, the other pretty complex).

You can use sbt to run it. Modify LTL.scala to change the run options. Command-line arguments are TODO.

Z3 is a requirement. `brew install z3` is sufficient on a Mac.
