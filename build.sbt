
name := "getblok_plasma"
organization := "io.getblok"
version := "0.5"
//idePackagePrefix := Some("io.getblok.getblok_plasma")
scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "org.ergoplatform" %% "ergo-appkit" % "develop-d90135c5-SNAPSHOT",
  "org.postgresql" % "postgresql" % "42.3.3",
  "org.scalatest" %% "scalatest" % "3.2.11" % "test",
  "io.swaydb" %% "swaydb" % "0.16.2",
  "io.swaydb" %% "boopickle" % "0.16.2",
  "org.scalatest" %% "scalatest" % "3.1.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.14.3" % "test",
  "org.scalatestplus" %% "scalatestplus-scalacheck" % "3.1.0.0-RC2" % Test,
  "com.storm-enroute" %% "scalameter" % "0.9" % "test",
  "org.slf4j" % "slf4j-jdk14" % "1.7.32",
)
libraryDependencies ++= Seq(
  "org.ethereum" % "leveldbjni-all"     % "1.18.3",
  "org.iq80.leveldb" % "leveldb" % "0.12",
)

resolvers ++= Seq(
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Bintray" at "https://jcenter.bintray.com/"
)
pomIncludeRepository := { _ => false }
assemblyJarName in assembly := s"plasma-${version.value}.jar"
assemblyOutputPath in assembly := file(s"./plasma-${version.value}.jar/")
