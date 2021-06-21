package splain

class SplainInternalError(detail: String, cause: Throwable = null)
    extends InternalError(
      "You've found a bug in splain formatting extension," +
        " please post this error with stack trace on https://github.com/tek/splain/issues\n\n" +
        detail,
      cause
    ) {}
