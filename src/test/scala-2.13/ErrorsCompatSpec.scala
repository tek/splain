package splain

class ErrorsCompatSpec
extends SpecBase
{
  def is = s2"""
  byname ${checkSuccess("byname")}
  """
}
