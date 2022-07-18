package io.chrisdavenport.snickerdoodle

import munit.CatsEffectSuite
import cats.effect._
import cats.syntax.all._
import org.http4s.implicits._
import org.http4s.ResponseCookie
import org.http4s.HttpDate
import io.chrisdavenport.publicsuffix.retrieval.client.PublicSuffixRetrieval

class SnCookieSpec extends CatsEffectSuite {


  test("SnCookie build will build for a domain"){
    val rc = ResponseCookie(
      "foo",
      "bar",
      None,
      None,
      domain = "foo.com".some,
      path = "/".some,
      None,
      false, 
      false,
      None,
    )
    val uri = uri"https://foo.com/"

    PublicSuffixRetrieval.getPublicSuffix[IO].map{ ps => 

      val cookie = SnCookie.build(rc, uri, HttpDate.MinValue.epochSecond, ps.isPublicSuffix)
      assertEquals(cookie.isDefined, true, s"Cookie is empty when it should be defined $cookie")
    }
  }


  test("SnCookie build will fail on a tld"){
    val rc = ResponseCookie(
      "foo",
      "bar",
      None,
      None,
      domain = "com".some,
      path = "/".some,
      None,
      false, 
      false,
      None,
    )
    val uri = uri"https://foo.com/"

    PublicSuffixRetrieval.getPublicSuffix[IO].map{ ps => 

      val cookie = SnCookie.build(rc, uri, HttpDate.MinValue.epochSecond, ps.isPublicSuffix)
      assertEquals(cookie.isEmpty, true, s"Cookie is defined when it should be none $cookie")
    }
  }


}