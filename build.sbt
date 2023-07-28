name := "image-website-backend"
organization := "Mrmi"

version := "1.0-SNAPSHOT"
lazy val root = (project in file(".")).enablePlugins(PlayScala, SwaggerPlugin)

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
  // Test
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test,
  "com.typesafe.slick" %% "slick" % "3.4.1",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",
  "com.github.tminglei" %% "slick-pg" % "0.21.1",
  "com.github.tminglei" %% "slick-pg_play-json" % "0.21.1",
  // TODO: logback error that I solved by using this specific version?
  "ch.qos.logback" % "logback-classic" % "1.3.0",
  // Minio
  "io.minio" % "minio" % "8.3.7", // This old version uses jackson 11 which fits scala version 2.13?
  "commons-io" % "commons-io" % "20030203.000550",
  "com.sksamuel.scrimage" % "scrimage-core" % "4.0.33",
  // Auth
  "com.github.jwt-scala" %% "jwt-play-json" % "9.4.3",
  "com.github.t3hnar" %% "scala-bcrypt" % "4.3.0",
  filters,
  // Elastic search
  "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % "8.8.1",
  // test kit
  "com.sksamuel.elastic4s" %% "elastic4s-testkit" % "8.8.1" % "test"
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
