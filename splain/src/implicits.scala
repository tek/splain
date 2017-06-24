package splain
package core

import tools.nsc._

import StringColor._

trait ImplicitChains
extends Formatting
{
  def featureImplicits: Boolean
  def featureBounds: Boolean
}
