package io.getblok.getblok_plasma.collections

import org.bouncycastle.util.encoders.Hex

case class Manifest(digest: Array[Byte], bytes: Array[Byte], subTrees: Seq[Array[Byte]]){
  def toHexString: (String, Seq[String]) = {
    Hex.toHexString(bytes) -> subTrees.map(Hex.toHexString)
  }

  override def toString: String = {
    val strings = toHexString
    s"${strings._1},${strings._2.slice(0, strings._2.size).map(_+",") + strings._2.lastOption.getOrElse("")}"
  }
}
