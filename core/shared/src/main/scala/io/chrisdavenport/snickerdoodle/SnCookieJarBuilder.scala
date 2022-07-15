package io.chrisdavenport.snickerdoodle

import cats.effect.kernel._
import cats.effect.std._
import cats.syntax.all._
import org.http4s.client.middleware.CookieJar
import fs2.io.file.Path

final class SnCookieJarBuilder[F[_]: Async] private (
  private val supervisorO: Option[Supervisor[F]],
  private val persistenceO: Option[SnCookiePersistence[F]]
){ self => 
  def copy(
  supervisorO: Option[Supervisor[F]] = self.supervisorO,
  persistenceO: Option[SnCookiePersistence[F]] = self.persistenceO
  ): SnCookieJarBuilder[F] = new SnCookieJarBuilder[F](supervisorO, persistenceO)

  def withSupervisor(s: Supervisor[F]) = copy(supervisorO = s.some)

  def withSqlitePersistence(path: Path) = 
    copy(persistenceO = SnCookiePersistence.sqlite(path).some)
  def withPersistence(c: SnCookiePersistence[F]) = 
    copy(persistenceO = c.some)

  def withoutPersistence = copy(persistenceO = None)

  def build: Resource[F, CookieJar[F]] = for {
    state <- Resource.eval(Ref[F].of(Map[SnCookie.SnCookieKey, SnCookie]()))
    tO <- persistenceO.traverse( p => 
      (Resource.eval(p.createTable >> p.getAll.flatMap{l => 
        val m = l.foldLeft(Map[SnCookie.SnCookieKey, SnCookie]()){
          case (m, c) => 
            val v = SnCookie.SnCookieKey.fromCookie(c) -> c
            m + v
        }
        state.set(m)
      }).as(p), supervisorO.fold(Supervisor[F])(_.pure[Resource[F, *]])).tupled
    )
    out = tO.fold[CookieJar[F]](new SnCookieJar.Http4sMemoryCookieJarImpl[F](state)){ case (cp, s) => new SnCookieJar.Http4sPersistenceCookieJarImpl[F](cp, s, state)}
    _ <- Resource.eval(out.evictExpired)
  } yield out

  def expert: Expert = new Expert()
  
  final class Expert(){
    def buildWithState: Resource[F, (CookieJar[F], Ref[F, Map[SnCookie.SnCookieKey, SnCookie]])] = for {
      state <- Resource.eval(Ref[F].of(Map[SnCookie.SnCookieKey, SnCookie]()))
      tO <- persistenceO.traverse( p => 
        (Resource.eval(p.createTable >> p.getAll.flatMap{l => 
        val m = l.foldLeft(Map[SnCookie.SnCookieKey, SnCookie]()){
          case (m, c) => 
            val v = SnCookie.SnCookieKey.fromCookie(c) -> c
            m + v
        }
        state.set(m)
      }).as(p), supervisorO.fold(Supervisor[F])(_.pure[Resource[F, *]])).tupled
      )
      cj = tO.fold[CookieJar[F]](new SnCookieJar.Http4sMemoryCookieJarImpl[F](state)){ case (cp, s) => new SnCookieJar.Http4sPersistenceCookieJarImpl[F](cp, s, state)}
      _ <- Resource.eval(cj.evictExpired)
    } yield (cj, state)
  }
}

object SnCookieJarBuilder {
  def default[F[_]: Async]: SnCookieJarBuilder[F] = new SnCookieJarBuilder[F](None, None)
}