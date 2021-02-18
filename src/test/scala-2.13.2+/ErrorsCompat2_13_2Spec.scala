package splain

class ErrorsCompat2_13_2Spec extends SpecBase {
  def is = s2"""
  ambiguous implicits ${checkError("ambiguous")}
  """
}
