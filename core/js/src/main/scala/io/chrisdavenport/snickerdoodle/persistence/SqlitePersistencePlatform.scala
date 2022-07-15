package io.chrisdavenport.snickerdoodle.persistence

import io.chrisdavenport.snickerdoodle.SnCookiePersistence
import cats.effect.kernel._


trait SqlitePersistencePlatform {

  def apply[F[_]: Async](path: fs2.io.file.Path): SnCookiePersistence[F] = 
    throw new RuntimeException("scala.js sqlite not yet implemented")
}