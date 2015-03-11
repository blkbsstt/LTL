LTL Model Checker
=================

This is a Scala library for specifying and model checking formulas in [Linear Temporal Logic](http://en.wikipedia.org/wiki/Linear_temporal_logic).

There is a DSL defined in Problem.scala that allows for a mildly succinct representation of complex LTL formulas.
You can find two example problems in Problem.scala as well (one quite simple, the other pretty complex).

You can use sbt to run it. Modify LTL.scala to change the run options. Command-line arguments are TODO.

Z3 is a requirement. `brew install z3` is sufficient on a Mac.

DSL
---

For an example of using the LTL DSL, consider the following formulation of a problem in which six people are attempting to cross a river in a boat that holds at most two people at a time.
Also, the six people are 3 couples, with jealous husbands, such that no woman may be around another man without also being around her husband.
The boat cannot cross the river without someone in it.

![boat problem](boat.png)

This can be encoded in the DSL as follows:

```scala
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
```
