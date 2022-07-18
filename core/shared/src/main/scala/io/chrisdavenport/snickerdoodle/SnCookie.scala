package io.chrisdavenport.snickerdoodle

import org.http4s.SameSite
import cats.syntax.all._
import org.http4s.{Uri, ResponseCookie, HttpDate}
import org.http4s.RequestCookie

case class SnCookie(
  name: String,
  value: String,
  domain: String,
  path: String,
  expiry: Long,
  lastAccessed: Long,
  creationTime: Long,
  isSecure: Boolean,
  isHttpOnly: Boolean,
  isHostOnly: Boolean,
  persist: Boolean,
  sameSite: SameSite,
  scheme: Option[Uri.Scheme],
  extension: Option[String],
)
object SnCookie {

  def build(rc: ResponseCookie, uri: Uri, now: Long, isPublicSuffix: String => Boolean): Option[SnCookie] = {
    // TODO filter public suffixes
    // https://publicsuffix.org
    val bail = rc.domain.map(d => isPublicSuffix(d) || !SnCookieJar.hostMatch(d, false, uri)).getOrElse(false)
    if (bail) Option.empty
    else uri.host.flatMap{ host => 
      val persist = rc.maxAge.isDefined || rc.expires.isDefined
      val expiry = rc.maxAge.map(now + _).orElse(rc.expires.map(_.epochSecond)).getOrElse(HttpDate.MaxValue.epochSecond)

      val path = rc.path.getOrElse{
          val s = uri.path.renderString
          if (s == "") "/" else s
        }

      val (domainS, isHostOnly) = rc.domain.map(d => (d, false))
        .getOrElse((host.renderString, true))

      SnCookie(
        name = rc.name,
        value = rc.content,
        domain = domainS.toLowerCase(),
        path = path.toLowerCase(),
        expiry = expiry,
        lastAccessed = now,
        creationTime = now,
        isSecure = rc.secure,
        isHttpOnly = rc.httpOnly,
        isHostOnly = isHostOnly,
        persist = persist,
        sameSite = rc.sameSite.getOrElse(SameSite.Lax),
        scheme = uri.scheme,
        extension = rc.extension,
      ).some
    }
  }

  case class SnCookieKey(
    name: String,
    domain: String,
    path: String
  )

  object SnCookieKey {
    def fromCookie(sn: SnCookie): SnCookieKey = SnCookieKey(sn.name, sn.domain, sn.path)
  }


  private[snickerdoodle] case class RawSnCookie(
    name: String,
    value: String,
    domain: String,
    path: String,
    expiry: Long,
    lastAccessed: Long,
    creationTime: Long,
    isSecure: Boolean,
    isHttpOnly: Boolean,
    isHostOnly: Boolean,
    sameSite: Int,
    scheme: Option[Int],
    extension: Option[String],
  )

  private def schemeInt(s: Uri.Scheme): Option[Int] = s.value match {
    case "http" => 1.some
    case "https" => 2.some
    case _ => None
  }

  private def intScheme(i: Int): Option[Uri.Scheme] = i match {
    case 1 => Uri.Scheme.http.some
    case 2 => Uri.Scheme.https.some
    case _ => None
  }

  private def sameSiteInt(s: SameSite): Int = s match {
    case SameSite.None => 1
    case SameSite.Lax => 2
    case SameSite.Strict => 3
  }

  private def intSameSite(i: Int): Option[SameSite] = i match {
    case 1 => SameSite.None.some
    case 2 => SameSite.Lax.some
    case 3 => SameSite.Strict.some
    case _ => None
  }

  private[snickerdoodle] def toRaw(cookie: SnCookie): RawSnCookie = RawSnCookie(
    cookie.name,
    cookie.value,
    cookie.domain,
    cookie.path,
    cookie.expiry,
    cookie.lastAccessed,
    cookie.creationTime,
    cookie.isSecure,
    cookie.isHttpOnly,
    cookie.isHostOnly,
    sameSiteInt(cookie.sameSite),
    cookie.scheme.flatMap(schemeInt),
    cookie.extension,
  )
  private[snickerdoodle] def fromRaw(cookie: RawSnCookie): SnCookie = SnCookie(
    cookie.name,
    cookie.value,
    cookie.domain,
    cookie.path,
    cookie.expiry,
    cookie.lastAccessed,
    cookie.creationTime,
    cookie.isSecure,
    cookie.isHttpOnly,
    cookie.isHostOnly,
    true,
    intSameSite(cookie.sameSite).get,
    cookie.scheme.flatMap(intScheme),
    cookie.extension,
  )

  def toRequestCookie(sn: SnCookie): RequestCookie = RequestCookie(sn.name, sn.value)
}