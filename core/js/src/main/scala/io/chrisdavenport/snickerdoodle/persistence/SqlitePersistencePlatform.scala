package io.chrisdavenport.snickerdoodle.persistence

import cats.syntax.all._
import io.chrisdavenport.snickerdoodle.SnCookie
import io.chrisdavenport.snickerdoodle.SnCookie.SnCookieKey
import io.chrisdavenport.snickerdoodle.SnCookiePersistence
import cats.effect.kernel._
import io.chrisdavenport.sqlitesjs.Sqlite
import io.circe.syntax._
import io.circe.Decoder
import io.circe.HCursor

trait SqlitePersistencePlatform {

  def apply[F[_]: Async](path: fs2.io.file.Path): Resource[F, SnCookiePersistence[F]] = 
    Sqlite.fromFile[F](path.toString).map(new SqlitePersistenceImpl[F](_))


  private class SqlitePersistenceImpl[F[_]: Async](sqlite: Sqlite[F]) extends SnCookiePersistence[F]{

    def updateLastAccessed(key: SnCookieKey, lastAccessed: Long): F[Unit] = {
      val updateStatement = "Update cookies SET lastAccessed = ? where name = ? and domain = ? and path = ?"
      sqlite.run(updateStatement, List(lastAccessed.asJson, key.name.asJson, key.domain.asJson, key.path.asJson))
        .void
    }

    def clear: F[Unit] = {
      val clearStatement = "DELETE FROM cookies"
      sqlite.exec(clearStatement)
    }
    def clearExpired(now: Long): F[Unit] = {
      val clearExpiredStatment = "DELETE FROM cookies where expiry < ?"
      sqlite.run(clearExpiredStatment, List(now.asJson)).void
    }
    def create(cookie: SnCookie): F[Unit] = {
      val raw = SnCookie.toRaw(cookie)
      val insertStatement = "INSERT OR REPLACE INTO cookies (name, value, domain, path, expiry, lastAccessed, creationTime, isSecure, isHttpOnly, isHostOnly, sameSite, scheme, extension) values (?,?,?,?,?,?,?,?,?,?,?,?,?)"
      sqlite.run(insertStatement, List(raw.name.asJson, raw.value.asJson, raw.domain.asJson, raw.path.asJson, raw.expiry.asJson, raw.lastAccessed.asJson, raw.creationTime.asJson, raw.isSecure.asJson, raw.isHttpOnly.asJson, raw.isHostOnly.asJson, raw.sameSite.asJson, raw.scheme.asJson, raw.extension.asJson))
        .void
    }

    def createTable: F[Unit] = {
      sqlite.exec(createTableStatement)
    }
    def getAll: F[List[SnCookie]] = {
      val selectStatement = "SELECT name,value,domain,path,expiry,lastAccessed,creationTime,isSecure,isHttpOnly, isHostOnly, sameSite,scheme,extension FROM cookies"
      sqlite.all(selectStatement).flatMap(
        l => l.traverse(_.as[SnCookie.RawSnCookie](rawDecoder).liftTo[F].map(SnCookie.fromRaw))
      )
    }

  }

  private val createTableStatement = {
    """CREATE TABLE IF NOT EXISTS cookies (
    name TEXT NOT NULL,
    value TEXT NOT NULL,
    domain TEXT NOT NULL,
    path TEXT NOT NULL,
    expiry INTEGER NOT NULL, -- Either MaxAge (relative to time called) or Expires (explicit) or HttpDate.MaxValue
    lastAccessed INTEGER NOT NULL,
    creationTime INTEGER NOT NULL,
    isSecure INTEGER NOT NULL, -- Boolean
    isHttpOnly INTEGER NOT NULL, -- Boolean
    isHostOnly INTEGER NOT NULL, -- Boolean
    sameSite INTEGER NOT NULL, --
    scheme INTEGER, 
    extension TEXT,
    CONSTRAINT cookiesunique UNIQUE (name, domain, path)
    )"""
  }

  private val rawDecoder = new Decoder[SnCookie.RawSnCookie]{
    def apply(c: HCursor): Decoder.Result[SnCookie.RawSnCookie] = (
      c.downField("name").as[String],
      c.downField("value").as[String],
      c.downField("domain").as[String],
      c.downField("path").as[String],
      c.downField("expiry").as[Long],
      c.downField("lastAccessed").as[Long],
      c.downField("creationTime").as[Long],
      c.downField("isSecure").as[Boolean](intBoolean),
      c.downField("isHttpOnly").as[Boolean](intBoolean),
      c.downField("isHostOnly").as[Boolean](intBoolean),
      c.downField("sameSite").as[Int],
      c.downField("scheme").as[Option[Int]],
      c.downField("extensione").as[Option[String]]
    ).mapN(SnCookie.RawSnCookie.apply)
  }

  private val intBoolean: Decoder[Boolean] = Decoder[Int].emap{
    case 0 => false.asRight
    case 1 => true.asRight
    case _ => "Invalid Integer Boolean".asLeft
  }
}