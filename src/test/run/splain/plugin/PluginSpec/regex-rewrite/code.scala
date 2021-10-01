object Level0
{
  object Level1
  {
    object Level2
    {
      type Type
    }
  }
}

object Run
{
  implicitly[Level0.Level1.Level2.Type]
}
