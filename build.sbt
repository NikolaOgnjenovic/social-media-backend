import com.typesafe.sbt.packager.docker.DockerVersion

name := "image-website-backend"
organization := "Mrmi"

version := "1.0-SNAPSHOT"
lazy val root =
  (project in file(".")).enablePlugins(PlayScala, SwaggerPlugin)

scalaVersion := "2.13.11"
swaggerDomainNameSpaces := Seq("models")

libraryDependencies ++= Seq(
  guice,
  // Database
  "com.typesafe.play" %% "play-slick" % "5.1.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.1.0",
  "org.postgresql" % "postgresql" % "42.5.4",
  "com.github.tminglei" %% "slick-pg" % "0.21.1", // Store lists in database
  // Swagger
  "org.webjars" % "swagger-ui" % "4.18.1",
  "ch.qos.logback" % "logback-classic" % "1.3.0",
  // Minio
  "io.minio" % "minio" % "8.3.7", // This old version uses jackson 11 which fits scala version 2.13?
  "commons-io" % "commons-io" % "20030203.000550",
  // Image compression
  "com.sksamuel.scrimage" % "scrimage-core" % "4.0.33",
  // Auth
  "com.github.jwt-scala" %% "jwt-play-json" % "9.4.3",
  // Password encryption
  "com.github.t3hnar" %% "scala-bcrypt" % "4.3.0",
  filters
)

dependencyOverrides += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.11.4"
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.1"

lazy val runLocal =
  taskKey[Unit]("Run the application with local configuration")
lazy val runDocker =
  taskKey[Unit]("Run the application with Docker configuration")

runLocal := {
  (Compile / run).toTask(s" -Dconfig.resource=application-local.conf").value
}

runDocker := {
  (Compile / run).toTask(s" -Dconfig.resource=application-docker.conf").value
}

dockerExposedPorts ++= Seq(9000, 9001, 9002)
dockerVersion := Some(DockerVersion(18, 9, 0, Some("ce")))
