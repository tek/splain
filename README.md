# A scala compiler plugin for more concise errors

This plugin removes some of the redundancy of the compiler output and prints
additional info for implicit resolution errors.

# Releases

### built-in - Scala 2.13.6+

Most features in splain 0.5.8 has been integrated into Scala 2.13.6 compiler through [this patch](https://github.com/scala/scala/pull/7785).

- Recommended if using Scala 2.13 and only splain 0.5.8 features. However, ... 
- **This integration is not 100%!** Configuration parameters have to be given new names to be compliant with the compiler standard. Many features are also discarded.

### v1.x (current) - Scala 2.13.6+

The above integration introduces a new compiler extension type (AnalyzerPlugin) that rendered most of old source code for splain v0.x incompatible or redundant. Thus, the team have decided to move on to the next major version, designed from scratch to have a cleaner architecture and better test coverage. Unfortunately, it will **not be available for Scala 2.13.5-**

- Recommended if using Scala 2.13 and the latest splain features/bugfixes.
- PRs and issues submitted for it will be given priority.

### v0.x (maintenance) - Scala 2.12, 2.13.5-

The latest v0.x will continue to be maintained and published regularly to stay compatible with the latest Scala 2.12.x release (until it's end-of-life), but no newer version will be published for Scala 2.13, **splain 0.5.8 will be the last release for Scala 2.13**.

If you are already using Scala 2.13, the team strongly recommend you to upgrade, and submit bug report and test cases directly to the latest v1.x.

- Recommended if using Scala 2.12.

### Build Matrix

| Version                     | Compatibility                                                                                |
|-----------------------------|----------------------------------------------------------------------------------------------|
| v1.x<br> (current) - latest | ![badge](https://github-actions.40ants.com/tek/splain/matrix.svg?branch=master)              |
| v1.0.0<br> (current)        | ![badge](https://github-actions.40ants.com/tek/splain/matrix.svg?branch=Release/1.0.0)    |
| v1.0.0-RC2<br> (current)    | ![badge](https://github-actions.40ants.com/tek/splain/matrix.svg?branch=Release/1.0.0-RC2)   |
| v1.0.0-RC1<br> (current)    | ![badge](https://github-actions.40ants.com/tek/splain/matrix.svg?branch=Release/1.0.0-RC1)   |
| v0.x<br> (maintenance) - latest | ![badge](https://github-actions.40ants.com/tek/splain/matrix.svg?branch=Maintenance%2Fmaster) |

# Usage

### v1.x, v0.x

Include this line in your `build.sbt` (_not_ `project/plugins.sbt`!!):

```sbt
addCompilerPlugin("io.tryp" % "splain" % "0.5.8" cross CrossVersion.patch)
```

If you want to support scala versions both newer and older than `2.12.5`, use:

```sbt
libraryDependencies += {
  val v =
    if (scalaVersion.value.replaceFirst(raw"\.(\d)$$",".0$1") <= "2.12.04") "0.4.1"
    else "0.5.8"
  ("io.tryp" %% "splain" % v cross CrossVersion.patch).withConfigurations(Some("plugin->default(compile)"))
}
```

If you are using gradle with scala plugin, include this line under the dependency section of your build.gradle:

```groovy
scalaCompilerPlugins group: 'io.tryp', name: 'splain_${scalaVersion}', version: '0.5.8'
```

or build.gradle.kts:

```kotlin
scalaCompilerPlugins("io.tryp:splain_${scalaVersion}:0.5.8")
```

### built-in

Do nothing! It is already built-in. Its 2 minimal features however has to be enabled manually, by the following 2 compiler arguments (see Configuration for details):

```
-Vimplicits -Vtype-diffs
```

# Configuration

The plugin can be configured via compiler arguments with the format:

| v0.x                          | built-in, v1.x       |
| :---------------------------- | -------------------- |
| `-P:splain:<param>[:<value>]` | `-<param>[:<value>]` |

`param` can be one of the following:

| v0.x              | built-in, v1.x                            | default value    |
| ----------------- | ----------------------------------------- | ---------------- |
| `all`             | (dropped)                                 |                  |
| `infix`           | (dropped)                                 |                  |
| `foundreq`        | `Vtype-diffs`                             |                  |
| `implicits`       | `Vimplicits`                              |                  |
| `bounds`          | (dropped)                                 | false            |
| `color`           | (dropped)                                 |                  |
| `breakinfix`      | (dropped)                                 | 0                |
| `tree`            | `Vimplicits-verbose-tree`                 |                  |
| `compact`         | (dropped)                                 | false            |
| `boundsimplicits` | (dropped)                                 |                  |
| `truncrefined`    | `Vimplicits-max-refined`                  | 0                |
| `rewrite`         | (dropped)                                 | (do not rewrite) |
| `keepmodules`     | (dropped)                                 | 0                |
| (N/A)             | `P:splain:Vimplicits-diverging`           | false            |
| (N/A)             | `P:splain:Vimplicits-diverging-max-depth` | 100              |

`value` can either be `true` or `false`. If omitted, the default is `true` for
both value and parameter.

The parameter `all` can be used to deactivate all features.

The parameters can be applied like this:

(in sbt)

```sbt
scalacOptions += "-P:splain:implicits:false"
```

(in gradle with scala plugin)

```kotlin
withType<ScalaCompile> {
    scalaCompileOptions.apply {
        additionalParameters = listOf("-P:splain:implicits:false")
    }
}
```

# infix types
Instead of `shapeless.::[A, HNil]`, prints `A :: HNil`.

# found/required types
Rather than printing up to four types, only the dealiased types are shown as a colored diff:

![foundreq](img/foundreq.jpg)

special consideration for `shapeless.Record`:

![foundreq_record](img/foundreq_record.jpg)

In the case of refined types in the form of `Client with Database with
Publisher`, the types will be matched with each other and a missing or surplus
type will be indicated by a `<none>` label.

# implicit resolution chains
When an implicit is not found, only the outermost error at the invocation point
is printed. This can be expanded with the compiler flag `-Xlog-implicits`, but
that also shows all invalid implicits for parameters that have been resolved
successfully.
This feature prints a compact list of all involved implicits:
![implicits](img/implicits.jpg)

Here, `!I` stands for *could not find implicit value*, the name of the implicit
parameter is in yellow, and its type in green.

If the parameter `tree` is set, the candidates will be indented according to their nesting level:

![tree](img/tree.jpg)

If the parameter `compact` is set, only the first and last implicit in a chain will be printed.

If the parameter `boundsimplicits` is set to false, any **nonconformant bounds** errors will be suppressed.

For comparison, this is the regular compiler output for this case (with
formatted types):

```
[info] unit/src/basic.scala:35: f is not a valid implicit value for
splain.ImplicitChain.T2 because:
[info] hasMatchingSymbol reported error: could not find implicit value for
parameter impPar2: (D *** (C *** String)) >:< ((C,D,C) *** D)
[info]   implicitly[T1]
[info]             ^
[info] unit/src/basic.scala:35: g is not a valid implicit value for
splain.ImplicitChain.T1 because:
[info] hasMatchingSymbol reported error: could not find implicit value for
parameter impPar1: D *** ((C >:< C) *** (D => Unit))
[info]   implicitly[T1]
[info]             ^
[error] unit/src/basic.scala:35: could not find implicit value for
parameter e: (C *** D) >:< C with D {type A = D; type B = C}
[error]   implicitly[T1]

```

# infix type and type argument line breaking

If the parameter `breakinfix` is given and greater than 0, types longer than
that number will be split into multiple lines:

```
implicit error;
!I e: String
f invalid because
!I impPar4: List[
  (
    VeryLongTypeName ::::
    VeryLongTypeName ::::
    VeryLongTypeName ::::
    VeryLongTypeName
  )
  ::::
  (Short :::: Short) ::::
  (
    VeryLongTypeName ::::
    VeryLongTypeName ::::
    VeryLongTypeName ::::
    VeryLongTypeName
  )
  ::::
  VeryLongTypeName ::::
  VeryLongTypeName ::::
  VeryLongTypeName ::::
  VeryLongTypeName
]
```

# truncating refined types

A type of the shape `T { type A = X; type B = Y }` will be displayed as `T {...}` if the parameter `truncrefined` is set
to a value `/= 0` and the refinement's length is greater than the value.

# truncating module paths

Default behaviour when printing type names is to omit the whole module path and only print the last segment.
Two options modify this behaviour:

## regex rewrite

The option `rewrite` takes a string that is parsed as a `;`-delimited list of regexes and optional replacements.

For example:

```
-P:splain:rewrite:cats\\.data/cd;.Type
```

This parses as two rewrite items:

* transform `cats.data` into `cd`
* delete all occurences of `.Type`

If a slash is present, the string following it will be used as a replacement for the matched text.
If it is absent, the empty string is substituted.

## dropping module segments by count

The option `keepmodules` determines how many segments of the module path before the type name will be displayed, but
only if the `rewrite` mechanism hasn't changed anything.

So with `-P:splain:keepmodules:2`, the qualified type `cats.free.FreeT.Suspend` will be displayed as
`free.FreeT.Suspend`, keeping the two segments `free.FreeT` before the type name.
The default is `0`, so only the type name itself will be displayed

# diverging implicit errors (experimental)

This error may be thrown by the Scala compiler if it cannot decide if an implicit search can terminate in polynomial time (e.g. if the search algorithm encounter a loop or infinite expansion). In most cases, such error will cause the entire search to fail immediately, but there are few exceptions to this rule, for which the search can backtrack and try an alternative path to fulfil the implicit argument. Either way, the Scala compiler error is only capable of showing the entry point of such loop or infinite expansion:

```
diverging implicit expansion for type splain.DivergingImplicits.C
starting with method f in object Circular
```

If the parameter `-P:splain:Vimplicits-diverging` is enabled, it will instruct the compiler to continue its implicit search process until an implicit resolution chain can be correlated with such error(s):

```
implicit error;
!I e: C
f invalid because
!I c: C
diverging implicit expansion for type C
starting with method f in object Endo
――f invalid because
  !I c: C
  diverging implicit expansion for type C
  starting with method f in object Endo
```

**WARNING!** This feature is marked "experimental" as sometimes it may cause failed implicit resolution to succeed, due to the delay in throwing the diverging implicit error. It may also increase compilation time slightly. If your build has been broken by this feature, please consider simplifying your code base to create a minimal reproducible test case, and submit it with a pull request.

# Development

## Bugs

Due to the nature of the hack that allows _splain_ to hook into the implicit search algorithm, other plugins using the
same trick may not work or cause _splain_ to be inactive.

Another victim of _splain_ is scaladoc – doc comments might disappear when running the task with _splain_ active, so
make sure it is disabled before doing so.

Users are encouraged to submit issues and test cases directly through pull requests, by forking the project and adding new test cases under:

| v0.x                                   | v1.x                                               |
| :------------------------------------- | -------------------------------------------------- |
| `<project root>/src/test/scala/splain` | `<project root>/core/src/test/scala/splain/plugin` |

The bug can thus be identified by the team quickly on our [continuous integration environment](https://github.com/tek/splain/actions). Submission on our GitHub issue tracker is also welcomed, but it generally takes much longer for the team to respond.

## How to compile

### v0.x (from git branch Maintenance/master)

Built with the latest stable [SBT](https://www.scala-sbt.org/). to compile and publish locally:

```
sbt clean publishM2
```

to run all tests:

```
sbt test
```

### v1.x (from git branch master)

Built with the latest [Gradle](https://gradle.org/), to compile and publish locally:

```
./gradlew clean testClasses publishToMavenLocal
```

to run all tests:

```
./gradlew test
```

## How to edit

Most project contributors uses neovim, IntelliJ IDEA or visual studio code.

The team strive for a strong discipline in software engineering. All commits (including SNAPSHOTs and PRs) will be compliant with [scalalfmt](https://scalameta.org/scalafmt/) standard.

## Communication

- @tek - reviewer for built-in/v0.x bugfix, new features
- @tribbloid - reviewer for v1.x bugfix
- @dwijnand - reviewer for scala compiler integration

