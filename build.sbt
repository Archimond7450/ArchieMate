import com.typesafe.sbt.packager.docker.*
import org.scalajs.linker.interface.ModuleSplitStyle
import org.scalajs.jsenv.nodejs.NodeJSEnv
import sbt.io.Path.relativeTo

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import scala.sys.process.Process

ThisBuild / scalaVersion := "3.3.4"

lazy val ArchieMate = (project in file("."))
  .aggregate(ArchieMateBackend, ArchieMateFrontend)
  .settings(publish / skip := true)

lazy val ArchieMateCross = crossProject(JVMPlatform, JSPlatform)
  .in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "ArchieMate",
    version := "0.1.2.1",
    libraryDependencies ++= Seq(
      // Circe
      "io.circe" %%% "circe-core" % "0.14.10",
      "io.circe" %%% "circe-jawn" % "0.14.10",
      "io.circe" %%% "circe-generic" % "0.14.10",
      "io.circe" %%% "circe-extras" % "0.14.1",
      "io.circe" %%% "circe-parser" % "0.14.10",

      // Scala test
      "org.scalatest" %%% "scalatest" % "3.2.9" % Test,

      // Scala mock
      "org.scalamock" %%% "scalamock" % "7.4.1" % Test
    ),
    testFrameworks += new TestFramework("org.scalatest.tools.Framework"),
    buildInfoKeys := Seq(
      name,
      version,
      scalaVersion,
      sbtVersion,
      BuildInfoKey.action("scalaJsVersion") {
        scalaJSVersion
      },
      BuildInfoKey.action("buildTime") {
        OffsetDateTime.now.format(DateTimeFormatter.ISO_DATE_TIME)
      }
    ),
    buildInfoPackage := "com.archimond7450.archiemate.build"
  )
  .jvmSettings(
    name := "ArchieMate Backend",
    libraryDependencies ++= Seq(
      // Pekko
      "org.apache.pekko" %% "pekko-actor-typed" % "1.2.0",
      "org.apache.pekko" %% "pekko-persistence" % "1.2.0",
      "org.apache.pekko" %% "pekko-persistence-typed" % "1.2.0",
      "org.apache.pekko" %% "pekko-persistence-testkit" % "1.2.0" % Test,
      "org.apache.pekko" %% "pekko-stream" % "1.2.0",
      "org.apache.pekko" %% "pekko-http" % "1.2.0",
      "org.apache.pekko" %% "pekko-http-spray-json" % "1.2.0",
      "org.apache.pekko" %% "pekko-http-testkit" % "1.2.0" % Test,
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % "1.2.0" % Test,
      "org.apache.pekko" %% "pekko-persistence-jdbc" % "1.1.1",
      "org.apache.pekko" %% "pekko-persistence-query" % "1.2.0",

      // Logback
      "ch.qos.logback" % "logback-classic" % "1.5.18",

      // Typesafe Config
      "com.typesafe" % "config" % "1.4.5",

      // PostgreSQL
      "org.postgresql" % "postgresql" % "42.7.7",

      // JWT
      "com.github.jwt-scala" %% "jwt-circe" % "11.0.3",

      // Circe
      "com.github.pjfanning" %% "pekko-http-circe" % "3.3.0"
    )
  )
  .jsSettings(
    name := "ArchieMate Frontend",
    libraryDependencies ++= Seq(
      // Laminar
      "com.raquo" %%% "laminar" % "17.2.0",

      // Routing
      "com.raquo" %%% "waypoint" % "10.0.0-M1"
    ),
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
    },
    jsEnv := new NodeJSEnv(),
    externalNpm := baseDirectory.value / ".." / "vite"
  )

lazy val ArchieMateFrontend = ArchieMateCross.js

lazy val ArchieMateBackend = ArchieMateCross.jvm
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    dockerBaseImage := "eclipse-temurin:17-jre",
    dockerExposedPorts := Seq(8080),
    dockerExposedVolumes := Seq("/opt/docker/logs"),
    dockerChmodType := DockerChmodType.UserGroupWriteExecute,
    dockerUpdateLatest := true,
    dockerUsername := Some("archimond7450"),
    Docker / packageName := "archiemate",
    Docker / daemonUser := "archiemate",
    // Docker / commands += Cmd("RUN", "mkdir", "-p", "/opt/docker/logs"),
    // Docker / commands += Cmd("RUN", "chmod", "-R", "740", "/opt/docker/logs"),
    Compile / mainClass := Some(
      "com.archimond7450.archiemate.ArchieMateBackend"
    ),
    Compile / resourceGenerators += Def.task {
      println("Frontend resource generator started")

      val base = (ThisBuild / baseDirectory).value
      val viteDir = base / "vite"
      val distDir = viteDir / "dist"
      val publicDir = (Compile / resourceManaged).value / "public"

      println("Running `yarn build` in vite")
      val yarnBuildCode = Process("yarn build", viteDir).!
      if (yarnBuildCode != 0) {
        throw new RuntimeException(
          s"Frontend resource generator failed because the 'yarn build' command failed with code $yarnBuildCode"
        )
      }

      println(s"Cleaning public dir $publicDir and copying dist dir $distDir")

      IO.delete(publicDir)
      IO.copyDirectory(distDir, publicDir)

      println("Frontend resource generator finished")

      (publicDir ** "*").get.filter(_.isFile)
    }.taskValue
  )

// lazy val buildDockerImage = ArchieMateBackend / Docker / publishLocal
// lazy val deployDockerImage = ArchieMateBackend / Docker / publish
// lazy val cleanDockerImage = ArchieMateBackend / Docker / clean
