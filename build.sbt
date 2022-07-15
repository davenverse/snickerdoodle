ThisBuild / tlBaseVersion := "0.0" // your current series x.y

ThisBuild / organization := "io.chrisdavenport"
ThisBuild / organizationName := "Christopher Davenport"
ThisBuild / licenses := Seq(License.MIT)
ThisBuild / developers := List(
  tlGitHubDev("christopherdavenport", "Christopher Davenport")
)
ThisBuild / tlCiReleaseBranches := Seq("main")
ThisBuild / tlSonatypeUseLegacyHost := true

val Scala213 = "2.13.7"
val Scala3 = "3.1.1"

ThisBuild / crossScalaVersions := Seq(Scala213, Scala3)
ThisBuild / scalaVersion := Scala213

ThisBuild / testFrameworks += new TestFramework("munit.Framework")

val catsV = "2.7.0"
val catsEffectV = "3.3.13"
val fs2V = "3.2.7"
val http4sV = "0.23.11"
val circeV = "0.14.2"
val doobieV = "1.0.0-RC2"
val munitCatsEffectV = "1.0.7"


// Projects
lazy val `snickerdoodle` = tlCrossRootProject
  .aggregate(core)

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .in(file("core"))
  .settings(
    name := "snickerdoodle",

    libraryDependencies ++= Seq(
      "org.typelevel"               %%% "cats-core"                  % catsV,
      "org.typelevel"               %%% "cats-effect"                % catsEffectV,

      "co.fs2"                      %%% "fs2-core"                   % fs2V,
      "co.fs2"                      %%% "fs2-io"                     % fs2V,
      "org.http4s"                  %%% "http4s-client"        % http4sV,

      "org.typelevel"               %%% "munit-cats-effect-3"        % munitCatsEffectV         % Test,
    )
  )
  .jsSettings(
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule)},
    // TODO Actually Implement with this
    // npmDependencies ++= Seq(
    //   "sqlite" -> "4.0.1",
    // ),
  ).jvmSettings(
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "doobie-core" % doobieV,
      "org.xerial"    %  "sqlite-jdbc" % "3.36.0.3",
    )
  )

lazy val examples = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .enablePlugins(NoPublishPlugin)
  .dependsOn(core)
  .in(file("examples"))
  .settings(
    name := "snickerdoodle-examples",
    libraryDependencies ++= Seq(
      "org.http4s"                  %%% "http4s-ember-client"        % http4sV,
    )
  )
  .jsSettings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule)},
  )

lazy val site = project.in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(core.jvm)
