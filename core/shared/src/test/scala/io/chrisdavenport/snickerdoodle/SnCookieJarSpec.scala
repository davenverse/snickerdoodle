package io.chrisdavenport.snickerdoodle

import munit.CatsEffectSuite
// import cats.effect._
import org.http4s.implicits._

class SnCookieJarSpec extends CatsEffectSuite {

  test("pathMatch should match an exact path match") {
    val test = SnCookieJar.pathMatch("/", uri"/")
    assertEquals(test, true)
  }

  test("pathMatch should match a valid deep path") {
    val test = SnCookieJar.pathMatch("/a", uri"/a/foo/bar")
    assertEquals(test, true)
  }

  test("pathMatch should match when path is root and uri is empty") {
    val test = SnCookieJar.pathMatch("/", uri"https://foo.bar")
    assertEquals(test, true)
  }

  test("pathMatch should match a valid when cookie ends with /") {
    val test = SnCookieJar.pathMatch("/a/", uri"/a/foo/bar")
    assertEquals(test, true)
  }

  test("pathMatch should fail to match a diferent prefix") {
    val test = SnCookieJar.pathMatch("/a", uri"/b/foo/bar")
    assertEquals(test, false)
  }

  test("hostMatch should match on an exact host match") {
    val test = SnCookieJar.hostMatch("foo.bar", false, uri"https://foo.bar/")
    assertEquals(test, true)
  }

  test("hostMatch should match on a subdomain host match") {
    val test = SnCookieJar.hostMatch("foo.bar", false, uri"https://baz.foo.bar/")
    assertEquals(test, true)
  }

  test("hostMatch should not match on a subdomain host match if host match is true") {
    val test = SnCookieJar.hostMatch("foo.bar", true, uri"https://baz.foo.bar/")
    assertEquals(test, false)
  }

  test("secureMatch should match on the same https scheme"){
    val test = SnCookieJar.secureMatch(true, uri"https://baz.bar/")
    assertEquals(test, true)
  }

  test("secureMatch should not match on the same http scheme"){
    val test = SnCookieJar.secureMatch(true, uri"http://baz.bar/")
    assertEquals(test, false)
  }

  test("secureMatch should match nonSecure http"){
    val test = SnCookieJar.secureMatch(false, uri"http://baz.bar/")
    assertEquals(test, true)
  }

  test("secureMatch should match nonSecure https"){
    val test = SnCookieJar.secureMatch(false, uri"https://baz.bar/")
    assertEquals(test, true)
  }

}