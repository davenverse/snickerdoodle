# snickerdoodle - Advanced Cookie Management  [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/snickerdoodle_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/snickerdoodle_2.13) ![Code of Conduct](https://img.shields.io/badge/Code%20of%20Conduct-Scala-blue.svg)

Cookies are a persistent stateful resource between a client and server.
However, maybe you run into an unexpected error, or someone sets a remember me token for a super long time, 
or maybe  you would just prefer to avoid work if you can because you have the information already available.

Snickerdoodle handles cookie persistence between application runs, it has a fully in memory implementation,
and will follow RFC 6265 correctly. Meaning it works very similarly to how your browser manages its cookies. Snickerdoodle is a bit more opinionated then the http4s implementation.

## [Head on over to the microsite](https://davenverse.github.io/snickerdoodle)

## Quick Start

To use snickerdoodle in an existing SBT project with Scala 2.13 or a later version, add the following dependencies to your
`build.sbt` depending on your needs:

```scala
libraryDependencies ++= Seq(
  "io.chrisdavenport" %% "snickerdoodle" % "<version>"
)
```
