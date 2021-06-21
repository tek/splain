package splain

class ErrorsSpec extends SpecBase {

  def is =
    s2"""
    implicit resolution chains ${checkError("chain")}
    found/required type diff ${checkError("foundreq")}
    nonconformant bounds ${checkError("bounds")}
    aux type ${checkError("aux")}
    shapeless Lazy ${checkError("lazy")}
    linebreak long infix types ${checkErrorWithBreak("break")}
    shapeless Record ${checkErrorWithBreak("record", 30)}
    deep hole ${checkError("deephole")}
    tree printing ${checkError("tree", "-P:splain:tree")}
    compact tree printing ${checkError(
        "tree",
        "-P:splain:tree -P:splain:compact",
        Some("errorCompact"),
      )}
    type prefix stripping ${checkError("prefix", "-P:splain:keepmodules:2")}
    regex type rewriting ${checkError(
        "regex-rewrite",
        "-P:splain:rewrite:\\.Level;0/5",
      )}
    refined type diff ${checkError("refined")}
    disambiguate types ${checkError("disambiguate")}
    truncate refined type ${checkError(
        "truncrefined",
        "-P:splain:truncrefined:10",
      )}
    byname higher order ${checkError("byname-higher")}
    Tuple1 ${checkError("tuple1")}
    single types ${checkError("single")}
    single types in function ${checkError("single-fn")}
    single types with free symbol ${checkError("single-free")}
    witness value types ${checkError("witness-value")}
    zio test ${checkError("zlayer")}
    """

//  def is = s2"""
//  single types ${checkError("witness-value")}
//  """
}
