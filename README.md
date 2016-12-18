# A scala compiler plugin for more concise errors
This plugin removes some of the redundancy of the compiler output and prints
additional info for implicit resolution errors.

# Usage

```sbt
resolvers += Resolver.url("bintray-tek", url("https://dl.bintray.com/tek/maven"))(Resolver.ivyStylePatterns)
addCompilerPlugin("tryp" %% "splain" % "0.1.0")
```

# Configuration
The plugin can be configured via compiler plugin parameters with the format:
```
-P:splain:<param>[:<value>]
```
`param` can be one of the following:
* `infix`
* `foundreq`
* `implicits`

`value` can either be `true` or `false`. If omitted, the default is `true` for
both value and parameter.

The parameters can be applied like this:

```sbt
scalacOptions += "-P:splain:implicits:false"
```

# infix types
Instead of `shapeless.::[A, HNil]`, prints `A :: HNil`.

# found/required types
Rather than printing up to four types, only the dealiased types are shown as a
colored diff:
![foundreq](img/foundreq.jpg)

# implicit resolution chains
When an implicit is not found, only the outermost error at the invocation point
is printed. This can be expanded with the compiler flag `-Xlog-implicits`, but
that also shows all invalid implicits for parameters that have been resolved
successfully.
This feature prints a compact list of all involved implicits:
![implicits](img/implicits.jpg)
