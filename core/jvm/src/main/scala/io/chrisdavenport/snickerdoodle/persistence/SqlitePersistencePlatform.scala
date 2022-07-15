package io.chrisdavenport.snickerdoodle
package persistence

import cats.effect.kernel._
import cats.syntax.all._

// TODO Become Platform when we have a js sqlite impl
trait SqlitePersistencePlatform { self => 
  import doobie._
  import doobie.syntax.all._
  private[snickerdoodle] val createTableStatement = {
    sql"""CREATE TABLE IF NOT EXISTS cookies (
    name TEXT NOT NULL,
    value TEXT NOT NULL,
    domain TEXT NOT NULL,
    path TEXT NOT NULL,
    expiry INTEGER NOT NULL, -- Either MaxAge (relative to time called) or Expires (explicit) or HttpDate.MaxValue
    lastAccessed INTEGER NOT NULL,
    creationTime INTEGER NOT NULL,
    isSecure INTEGER, -- Boolean
    isHttpOnly INTEGER, -- Boolean
    isHostOnly INTEGER, -- Boolean
    sameSite INTEGER NOT NULL, --
    scheme INTEGER, 
    extension TEXT,
    CONSTRAINT cookiesunique UNIQUE (name, domain, path)
    )"""
  }
/*

*/

  private[snickerdoodle] def transactor[F[_]: Async](path: fs2.io.file.Path): Transactor[F] = {
    Transactor.fromDriverManager[F]("org.sqlite.JDBC", s"jdbc:sqlite:${path.toString}", "", "")
  }

  private[snickerdoodle] def createTable[F[_]: MonadCancelThrow](xa: Transactor[F]): F[Unit] = {
    createTableStatement.update.run.transact(xa).void
  }

  private[snickerdoodle] def selectAll[F[_]: MonadCancelThrow](xa: Transactor[F]) = {
    sql"SELECT name,value,domain,path,expiry,lastAccessed,creationTime,isSecure,isHttpOnly, isHostOnly, sameSite,scheme,extension FROM cookies".query[SnCookie.RawSnCookie]
      .to[List]
      .transact(xa)
      .map(_.map(SnCookie.fromRaw))
  }

  private[snickerdoodle] def updateLastAccessed[F[_]: MonadCancelThrow](xa: Transactor[F])(key: SnCookie.SnCookieKey, lastAccessed: Long): F[Int] = {
    Update[(Long, SnCookie.SnCookieKey)]("Update cookies SET lastAccessed = ? where name = ? and domain = ? and path = ?")
      .run((lastAccessed, key))
      .transact(xa)
  }

  private[snickerdoodle] def create[F[_]: MonadCancelThrow](xa: Transactor[F])(cookie: SnCookie): F[Int] = {
    Update[SnCookie.RawSnCookie]("INSERT OR REPLACE INTO cookies values (?,?,?,?,?,?,?,?,?,?,?,?,?)")
      .run(SnCookie.toRaw(cookie))
      .transact(xa)
  }

  private[snickerdoodle] def delete[F[_]: MonadCancelThrow](xa: Transactor[F])(key: SnCookie.SnCookieKey): F[Int] = {
    Update[SnCookie.SnCookieKey]("DELETE FROM cookies where name = ? and domain = ? and path = ?")
      .run(key)
      .transact(xa)
  }

  private[snickerdoodle] def clear[F[_]: MonadCancelThrow](xa: Transactor[F]): F[Int] = {
    sql"DELETE FROM cookies"
      .update
      .run
      .transact(xa)
  }

  private[snickerdoodle] def clearExpired[F[_]: MonadCancelThrow](xa: Transactor[F])(now: Long): F[Int] = {
    Update[Long]("DELETE FROM cookies where expiry < ?")
      .run(now)
      .transact(xa)
  }

  def apply[F[_]: Async](path: fs2.io.file.Path): SnCookiePersistence[F] = {
    val xa = transactor[F](path)
    new SnCookiePersistence[F] {
      def updateLastAccessed(key: SnCookie.SnCookieKey, lastAccessed: Long): F[Unit] =
        self.updateLastAccessed(xa)(key, lastAccessed).void
      
      // def delete(key: SnCookieKey): F[Unit] = 
      //   self.delete(xa)(key).void
      
      def clear: F[Unit] = 
        self.clear(xa).void

      def clearExpired(now: Long): F[Unit] = self.clearExpired(xa)(now).void
      
      def create(cookie: SnCookie): F[Unit] = self.create(xa)(cookie).void
      
      def createTable: F[Unit] = self.createTable(xa)
      
      def getAll: F[List[SnCookie]] = self.selectAll(xa)
      
    }
  }
}