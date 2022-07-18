package io.chrisdavenport.snickerdoodle

import cats.effect.kernel._
import cats.syntax.all._

private trait SnCookieJarBuilderDefaultsPlaftom {
  def isPublicSuffix[F[_]: Async]: Option[Resource[F, String => Boolean]] = 
    Resource.eval(io.chrisdavenport.publicsuffix.retrieval.client.PublicSuffixRetrieval.getPublicSuffix)
      .map(ps => {(s: String) => ps.isPublicSuffix(s)})
      .some
}