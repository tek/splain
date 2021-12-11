object FoundReq
{
  object F
  val a = new (Int *** ((List[C] *** C { type A = C }) >:< D))
  def f(arg: Int *** ((List[String] *** C { type A = F.type }) >:< D)) = ???
  f(a)
}
