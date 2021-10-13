package splain

class SplainInternalError(message: String, cause: Throwable = null) extends InternalError(message, cause) {}
