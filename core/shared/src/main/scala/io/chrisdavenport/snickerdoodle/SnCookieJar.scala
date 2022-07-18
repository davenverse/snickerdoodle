package io.chrisdavenport.snickerdoodle

import cats._
import cats.effect._
import cats.effect.std._
import org.http4s._
import cats.syntax.all._
import org.http4s.client.middleware.CookieJar

private[snickerdoodle] object SnCookieJar {

  private[snickerdoodle] class Http4sPersistenceCookieJarImpl[F[_]: Concurrent: Clock](
    persistence: SnCookiePersistence[F],
    supervisor: Supervisor[F],
    ref: Ref[F, Map[SnCookie.SnCookieKey, SnCookie]],
    isPublicSuffix: String => Boolean
  ) extends CookieJar[F]{
    def evictExpired: F[Unit] = for {
      now <- HttpDate.current[F].map(_.epochSecond)
      _ <- ref.update(_.filter(t => now <= t._2.expiry))
      _ <- supervisor.supervise(persistence.clearExpired(now))
    } yield ()
    
    def evictAll: F[Unit] = 
      ref.set(Map.empty) >> 
      persistence.clear
    
    def addCookies[G[_]: Foldable](cookies: G[(ResponseCookie, Uri)]): F[Unit] = 
      for {
        now <- HttpDate.current[F].map(_.epochSecond)
        map = cookies.foldLeft(Map[SnCookie.SnCookieKey, SnCookie]()){
          case (m, (rc, uri)) =>
            val snO = SnCookie.build(rc, uri, now, isPublicSuffix).map( sn => 
              (SnCookie.SnCookieKey.fromCookie(sn), sn)
            )
            snO.fold(m)(v => m + v)
        }
        _ <- ref.update(m => m ++ map)
        _ <- supervisor.supervise(map.toList.traverse_{ case (_, c) => if (c.persist) persistence.create(c) else Applicative[F].unit}).void
      } yield ()
    
    def enrichRequest[G[_]](r: Request[G]): F[Request[G]] = for {
      now <- HttpDate.current[F].map(_.epochSecond)
      cookies <- ref.modify{ m => 
        val cookies = m.toList.map(_._2)
        val filtered = cookies.filter(c => cookieAppliesToRequest(r, c)).map(_.copy(lastAccessed = now))
        val updatedMap = filtered.foldLeft(Map[SnCookie.SnCookieKey, SnCookie]()){
          case (m, c) => 
            val v = SnCookie.SnCookieKey.fromCookie(c) -> c
            m + v
        }
        (m ++ updatedMap, filtered)
      }
      _ <- supervisor.supervise(cookies.traverse_(c => if (c.persist) persistence.updateLastAccessed(SnCookie.SnCookieKey.fromCookie(c),  c.lastAccessed) else Applicative[F].unit))
    } yield cookies.foldLeft(r){ case (r, c) => r.addCookie(SnCookie.toRequestCookie(c))}
    
  }


  // TODO Make this into http4s proper
  private[snickerdoodle] class Http4sMemoryCookieJarImpl[F[_]: Concurrent: Clock](
    ref: Ref[F, Map[SnCookie.SnCookieKey, SnCookie]],
    isPublicSuffix: String => Boolean
  ) extends CookieJar[F]{
    def evictExpired: F[Unit] = for {
      now <- HttpDate.current[F]
      _ <- ref.update(_.filter(t => now.epochSecond <= t._2.expiry))
    } yield ()
    
    def evictAll: F[Unit] = ref.set(Map.empty)
    
    def addCookies[G[_]: Foldable](cookies: G[(ResponseCookie, Uri)]): F[Unit] = 
      for {
        now <- HttpDate.current[F].map(_.epochSecond)
        map = cookies.foldLeft(Map[SnCookie.SnCookieKey, SnCookie]()){
          case (m, (rc, uri)) =>
            val snO = SnCookie.build(rc, uri, now, isPublicSuffix).map( sn => 
              (SnCookie.SnCookieKey.fromCookie(sn), sn)
            )
            snO.fold(m)(v => m + v)
        }
        _ <- ref.update(m => m ++ map)
      } yield ()
    
    def enrichRequest[G[_]](r: Request[G]): F[Request[G]] = for {
      now <- HttpDate.current[F].map(_.epochSecond)
      cookies <- ref.modify{ m => 
        val cookies = m.toList.map(_._2)
        val filtered = cookies.filter(c => cookieAppliesToRequest(r, c)).map(_.copy(lastAccessed = now))
        val updatedMap = filtered.foldLeft(Map[SnCookie.SnCookieKey, SnCookie]()){
          case (m, c) => 
            val v = SnCookie.SnCookieKey.fromCookie(c) -> c
            m + v
        }
        (m ++ updatedMap, filtered)
      }
    } yield cookies.foldLeft(r){ case (r, c) => r.addCookie(SnCookie.toRequestCookie(c))}
    
  }


  private[snickerdoodle] def cookieAppliesToRequest[F[_]](
    r: Request[F],
    c: SnCookie,
  ): Boolean = {
    val rp = r.uri    
    hostMatch(c.domain, c.isHostOnly, rp) &&
    pathMatch(c.path, rp) &&
    secureMatch(c.isSecure, rp) 
  }

  private[snickerdoodle] def hostMatch(domain: String, isHostOnly: Boolean, uri: Uri): Boolean = {
    uri.host.map{host => 
      val hostS = host.renderString.toLowerCase()
      // The domain string and the string are identical.  
      hostS == domain || !isHostOnly && {
        // The domain string is a suffix of the string.
        val i = hostS.indexOf(domain)
        (i != -1) && // Does Exist
        hostS.isDefinedAt(i - 1) && hostS.charAt(i - 1) == '.' &&  
        // The last character of the string that is not included in the domain string is a '.'
        hostS.length() - i == domain.length() // Is A Suffix
      }
    }.getOrElse(false)
  }

  /*
   o  The cookie-path and the request-path are identical.

   o  The cookie-path is a prefix of the request-path, and the last
      character of the cookie-path is %x2F ("/").

   o  The cookie-path is a prefix of the request-path, and the first
      character of the request-path that is not included in the cookie-
      path is a %x2F ("/") character.
  */
  private[snickerdoodle] def pathMatch(cookiePath: String, uri: Uri): Boolean = {
    val pathI = uri.path.renderString.toLowerCase()
    val path = if (pathI == "") "/" else pathI
    cookiePath == path || path.startsWith(cookiePath) && {
      cookiePath.endsWith("/") || 
      path.drop(cookiePath.length()).headOption == Some('/')
    }
  }

  private[snickerdoodle] def secureMatch(isSecure: Boolean, uri: Uri): Boolean = {
    if (isSecure) uri.scheme.exists(s => s === Uri.Scheme.https)
    else true
  }


}