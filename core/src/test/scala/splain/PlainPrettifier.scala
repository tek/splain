package splain

import org.scalactic.{Prettifier, PrettyPair}

object PlainPrettifier extends Prettifier {

  override def apply(o: Any): String = Prettifier.basic.apply(o)

  override def apply(left: Any, right: Any): PrettyPair = PrettyPair(apply(left), apply(right), None)
}
