package io.chrisdavenport.snickerdoodlexample

import cats.effect._
import cats.effect.std.Console
import cats.syntax.all._
import org.http4s._
import org.http4s.implicits._
import org.http4s.client.middleware.CookieJar
import org.http4s.ember.client.EmberClientBuilder
import io.chrisdavenport.snickerdoodle._
// import scala.concurrent.duration._

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    (
      SnCookieJarBuilder.default[IO]
        .withSqlitePersistence(fs2.io.file.Path("sample.sqlite")) // Comment this line for JS
        .expert // Usually you would just use `build` we use buildWithState to expose the internals
        .buildWithState,
      EmberClientBuilder.default[IO].build
    ).mapN{ case ((cj, state), c) => (CookieJar(cj)(c), state)} // Application uses the Http4s Middleware
    .use{ case (client,state) =>
      IO.println("Cookie Walkthrough") >>
      IO.println("") >>
      IO.println("Initial State") >>
      state.get.map(_.toList.map(_._2)).flatTap(_.traverse_(Console[IO].println(_))) >>
      client.status(Request[IO](Method.GET, uri"https://httpbin.org/cookies/set?foo=baz&zed=zoom&zam=zoop")) >>
      IO.println("") >>
      IO.println("GET https://httpbin.org/cookies/set?foo=baz&zed=zoom&zam=zoop") >>
      IO.println("") >> 
      IO.println("Httpbin Cookies are Session Cookies so they Are Not Persisted, but they are stored locally") >>
      state.get.map(_.toList.map(_._2)).flatTap(_.traverse_(Console[IO].println(_))) >>
      IO.println("") >>
      IO.println("But they are sent on requests within the same session") >>
      IO.println("") >>
      IO.println("GET https://httpbin.org/cookies") >>
      IO.println("") >>
      client.expect[String](Request[IO](Method.GET, uri"https://httpbin.org/cookies")).flatTap(Console[IO].println(_)) >>
      IO.println("") >>
      IO.println("Reddit However Sets a Persistent Cookie On Visiting the page") >>
      IO.println("") >>
      IO.println("GET https://www.reddit.com/") >>
      client.status(Request[IO](Method.GET, uri"https://www.reddit.com/")) >>
      IO.println("") >> 
      IO.println("These are both locally present") >>
      state.get.map(_.toList.map(_._2)).flatTap(_.traverse_(Console[IO].println(_)))
    }  >>
    {
      IO.println("") >> 
      // Comment this block for JS
      IO.println("As well as persisted to disk in the sqlite database.") >>
      SnCookiePersistence.sqlite[IO](fs2.io.file.Path("sample.sqlite"))
        .getAll
        .flatTap(_.traverse_(Console[IO].println(_))) >>
      IO.println("") >>
      //
      IO.println("Run Again, and you will see these cookies are restored in the initial state") >>
      IO.println("")
    }
  }.as(ExitCode.Success)


}