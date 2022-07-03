package io.getblok.getblok_plasma.collections

import org.bouncycastle.util.encoders.Hex

case class Manifest(digest: Array[Byte], bytes: Array[Byte], subtrees: Seq[Array[Byte]]){
  def toHexString: (String, Seq[String]) = {
    Hex.toHexString(bytes) -> subtrees.map(Hex.toHexString)
  }
}
