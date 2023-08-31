object Ambiguous {
  implicit val c1: C = ???
  implicit val c2: C = ???
  implicit def f1: D = ???
  implicit def f2: D = ???
  implicitly[D]
}
