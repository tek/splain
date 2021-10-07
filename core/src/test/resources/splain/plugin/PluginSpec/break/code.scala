object InfixBreak
{
  type ::::[A, B]
  trait VeryLongTypeName
  trait Short
  type T1 = VeryLongTypeName :::: VeryLongTypeName :::: VeryLongTypeName ::::
    VeryLongTypeName
  type T2 = T1 :::: (Short :::: Short) :::: T1 :::: T1
  implicit def f(implicit impPar4: List[T2]): String = ???
  implicitly[String]
}
