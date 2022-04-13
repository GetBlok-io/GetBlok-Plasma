
name := "getblok_plasma"
organization := "io.getblok"
version := "0.5"
idePackagePrefix := Some("io.getblok.getblok_plasma")
scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "org.ergoplatform" %% "ergo-appkit" % "develop-d90135c5-SNAPSHOT",
  "org.postgresql" % "postgresql" % "42.3.3",
  "org.scalatest" %% "scalatest" % "3.2.11" % "test",
  "org.slf4j" % "slf4j-simple" % "1.7.36"
)

resolvers ++= Seq(
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

//assemblyJarName in assembly := s"subpooling-${version.value}.jar"
//mainClass in assembly := Some("app.SubpoolMain")
//assemblyOutputPath in assembly := file(s"./subpooling-${version.value}.jar/")
