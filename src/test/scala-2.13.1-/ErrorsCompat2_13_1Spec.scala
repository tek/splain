package splain

class ErrorsCompat2_13_1Spec
extends SpecBase
{
  def is = s2"""
  ambiguous implicits ${checkError("ambiguous-2.13.1-")}
  """
}
