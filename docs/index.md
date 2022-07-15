# snickerdoodle - Advanced Cookie Management [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/snickerdoodle_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/snickerdoodle_2.13)

## Quick Start

To use snickerdoodle in an existing SBT project with Scala 2.13 or a later version, add the following dependencies to your
`build.sbt` depending on your needs:

```scala
libraryDependencies ++= Seq(
  "io.chrisdavenport" %% "snickerdoodle" % "<version>"
)
```

## How to use

```scala mdoc
import cats.syntax.all._
import cats.effect._
import fs2.io.file.Path
import io.chrisdavenport.snickerdoodle._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.client.middleware.CookieJar

// Create the cookie jar like so
val jarResource = SnCookieJarBuilder.default[IO]
  .withSqlitePersistence(Path("sample.sqlite"))
  .build

// Typical way you generally make a client
val clientResource = EmberClientBuilder.default[IO].build


val combined = (jarResource, clientResource).mapN{
  // Apply it to a client
  case (jar, client) => CookieJar(jar)(client)
}
```