name := "aconcagua"

val aconcaguaVersion = "0.0.2"

val priceVersion   = "0.0.2"
val stdListVersion = "0.0.2"

val mainScala = "2.13.4"
val allScala  = Seq(mainScala)

val calibanVersion = "0.9.4"
val uzhttpVersion  = "0.2.6"
val zioVersion     = "1.0.3"
val zioZmxVersion  = "0.0.4+69-01a7e756-SNAPSHOT"

inThisBuild(
  List(
    organization := "dev.palanga",
    homepage := Some(url("https://github.com/palanga/aconcagua")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    parallelExecution in Test := false,
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/palanga/aconcagua/"),
        "scm:git:git@github.com:palanga/aconcagua.git",
      )
    ),
    developers := List(
      Developer(
        "palanga",
        "Andrés González",
        "a.gonzalez.terres@gmail.com",
        url("https://github.com/palanga"),
      )
    ),
    publishTo := Some("Artifactory Realm" at "https://palanga.jfrog.io/artifactory/maven/"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val root =
  (project in file("."))
    .settings(skip in publish := true)
    .aggregate(
      core,
      price,
      serverExamples,
      stdList,
    )

lazy val price =
  (project in file("price"))
    .settings(commonSettings)
    .settings(
      name := "price",
      version := priceVersion,
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio-test"     % zioVersion % "test",
        "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
      ),
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      fork in Test := true,
      fork in run := true,
    )
    .dependsOn(
      stdList
    )

lazy val core =
  (project in file("core"))
    .settings(commonSettings)
    .settings(
      name := "aconcagua",
      version := aconcaguaVersion,
      libraryDependencies ++= Seq(
        "com.github.ghostdogpr" %% "caliban"        % calibanVersion,
        "com.github.ghostdogpr" %% "caliban-http4s" % calibanVersion,
        "dev.zio"               %% "zio-zmx"        % zioZmxVersion,
        "org.polynote"          %% "uzhttp"         % uzhttpVersion,
      ),
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      fork in Test := true,
      fork in run := true,
    )

lazy val serverExamples =
  (project in file("examples/server"))
    .settings(commonSettings)
    .settings(
      name := "server-examples",
      libraryDependencies ++= Seq(
        "ch.qos.logback" % "logback-classic" % "1.2.3"
      ),
      fork in Test := true,
      fork in run := true,
      skip in publish := true,
    )
    .dependsOn(
      core
    )

lazy val stdList =
  (project in file("std/list"))
    .settings(commonSettings)
    .settings(
      name := "std.list",
      version := stdListVersion,
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio-test"     % zioVersion % "test",
        "dev.zio" %% "zio-test-sbt" % zioVersion % "test",
      ),
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
      fork in Test := true,
      fork in run := true,
    )

val commonSettings =
  Def.settings(
    scalaVersion := mainScala,
    crossScalaVersions := allScala,
    libraryDependencies += compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    resolvers += "Artifactory" at "https://palanga.jfrog.io/artifactory/maven/",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-explaintypes",
      "-Yrangepos",
      "-feature",
      "-language:higherKinds",
      "-language:existentials",
      "-unchecked",
      "-Xlint:_,-type-parameter-shadow",
      //    "-Xfatal-warnings",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused:patvars,-implicits",
      "-Ywarn-value-discard",
      //      "-Ymacro-annotations",
    ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) =>
        Seq(
          "-Xsource:2.13",
          "-Yno-adapted-args",
          "-Ypartial-unification",
          "-Ywarn-extra-implicit",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-opt-inline-from:<source>",
          "-opt-warnings",
          "-opt:l:inline",
        )
      case _             => Nil
    }),
    //    scalacOptions in Test --= Seq("-Xfatal-warnings"),
  )
