package io.chrisdavenport.snickerdoodle

import cats.effect.kernel._
import SnCookie.SnCookieKey

trait SnCookiePersistence[F[_]]{
  def updateLastAccessed(key: SnCookieKey, lastAccessed: Long): F[Unit]
  // def delete(key: SnCookieKey): F[Unit]
  def clear: F[Unit]
  def clearExpired(now: Long): F[Unit]
  def create(cookie: SnCookie): F[Unit]

  // Create table if not exists
  def createTable: F[Unit]
  def getAll: F[List[SnCookie]]
}


object SnCookiePersistence {
  def sqlite[F[_]: Async](path: fs2.io.file.Path): Resource[F, SnCookiePersistence[F]] = persistence.SqlitePersistence[F](path)
}