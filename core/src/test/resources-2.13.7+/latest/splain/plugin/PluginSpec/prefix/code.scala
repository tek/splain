object Level0
{
  object Level1
  {
    object Level2
    {
      type Type[F[_]]
      type Par[A]
    }
  }

  implicitly[Level0.Level1.Level2.Type[Level0.Level1.Level2.Par]]
}
