package io.getblok.getblok_plasma

import scorex.crypto.authds.avltree.batch.NodeParameters


case class PlasmaParameters(keySize: Int, valueSizeOpt: Option[Int]){
  def toNodeParams: NodeParameters = {
    NodeParameters(keySize, valueSizeOpt, keySize)
  }
}

object PlasmaParameters {
  lazy val default: PlasmaParameters = PlasmaParameters(32, None)
}