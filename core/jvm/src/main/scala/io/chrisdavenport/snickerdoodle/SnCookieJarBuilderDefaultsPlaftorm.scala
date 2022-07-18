package io.chrisdavenport.snickerdoodle

import cats.effect.kernel._
import cats.syntax.all._

private trait SnCookieJarBuilderDefaultsPlaftom {
  def isPublicSuffix[F[_]: Async]: Option[Resource[F, String => Boolean]] = 
    {(s: String) => io.chrisdavenport.publicsuffix.PublicSuffix.global.isPublicSuffix(s)}
      .pure[Resource[F, *]]
      .some
}