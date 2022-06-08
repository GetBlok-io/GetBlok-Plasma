package io.getblok.getblok_plasma

case class PlasmaParameters(keySize: Int, valueSizeOpt: Option[Int])

object PlasmaParameters {
  lazy val default: PlasmaParameters = PlasmaParameters(32, None)
}