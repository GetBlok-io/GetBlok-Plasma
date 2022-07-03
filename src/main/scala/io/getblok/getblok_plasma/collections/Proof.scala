package io.getblok.getblok_plasma.collections

import org.bouncycastle.util.encoders.Hex
import org.ergoplatform.appkit.{ErgoType, ErgoValue}
import scorex.crypto.authds
import sigmastate.eval.Colls
import special.collection.Coll
import supertagged.@@

case class Proof(bytes: Array[Byte]){
  lazy val ergoType: ErgoType[Byte]         = ErgoType.byteType()
  def ergoValue: ErgoValue[Coll[Byte]]      = ErgoValue.of(Colls.fromArray(bytes), ergoType)

  def toADProof: Array[Byte] @@ authds.SerializedAdProof.Tag = authds.SerializedAdProof @@ bytes

  override def toString: String = Hex.toHexString(bytes).toLowerCase
}
