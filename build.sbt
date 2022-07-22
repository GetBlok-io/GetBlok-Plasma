import sbt.url
import xerial.sbt.Sonatype.GitHubHosting

name := "getblok_plasma"
organization := "io.github.getblok-io"
version := "0.0.4"
//idePackagePrefix := Some("io.getblok.getblok_plasma")
scalaVersion := "2.12.10"
ThisBuild / version      := "0.0.4"
libraryDependencies ++= Seq(
  "org.ergoplatform" %% "ergo-appkit" % "4.0.10",
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
  "javax.xml.bind" % "jaxb-api" % "2.4.0-b180830.0359",
  "org.ethereum" % "leveldbjni-all"     % "1.18.3",
  "org.iq80.leveldb" % "leveldb" % "0.12",
)

resolvers ++= Seq(
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Bintray" at "https://jcenter.bintray.com/"
)

assemblyMergeStrategy in assembly := {
  case "logback.xml" => MergeStrategy.first
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case PathList("org", "bouncycastle", xs @ _*) => MergeStrategy.first
  case PathList("org", "iq80", "leveldb", xs @ _*) => MergeStrategy.first
  case PathList("org", "bouncycastle", xs @ _*) => MergeStrategy.first
  case PathList("javax", "activation", xs @ _*) => MergeStrategy.last
  case PathList("javax", "annotation", xs @ _*) => MergeStrategy.last
  case other => (assemblyMergeStrategy in assembly).value(other)
}



assemblyJarName in assembly := s"plasma-${version.value}.jar"
assemblyOutputPath in assembly := file(s"./plasma-${version.value}.jar/")
