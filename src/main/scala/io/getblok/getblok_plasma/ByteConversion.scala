package io.getblok.getblok_plasma

import com.google.common.primitives.Longs
import io.getblok.getblok_plasma.persistence.Types.{PlasmaKey, PlasmaVal, VersionedDigest}
import org.bouncycastle.util.encoders.Hex
import org.ergoplatform.appkit.{Address, ErgoId}
import scorex.crypto.authds.{ADDigest, ADKey, ADValue}
import sigmastate.Values
import sigmastate.serialization.ErgoTreeSerializer
import supertagged.@@

trait ByteConversion[T] {

  def convertToBytes(t: T): Array[Byte]
  def convertFromBytes(bytes: Array[Byte]): T

  def toKey(t: T): PlasmaKey = PlasmaKey(convertToBytes(t))
  def toVal(t: T): PlasmaVal = PlasmaVal(convertToBytes(t))

  def toADKey(t: T):    ADKey    = ADKey @@ convertToBytes(t)
  def toADVal(t: T):    ADValue  = ADValue @@ convertToBytes(t)
  def toADDigest(t: T): ADDigest = ADDigest @@ convertToBytes(t)

  def ofKey(pKey: PlasmaKey): T = convertFromBytes(pKey.key)
  def ofVal(pVal: PlasmaVal): T = convertFromBytes(pVal.value)
}

object ByteConversion {
  implicit val convertsString: ByteConversion[String] = new ByteConversion[String] {
    override def convertToBytes(t: String): Array[Byte] = Hex.decode(t)

    override def convertFromBytes(bytes: Array[Byte]): String = Hex.toHexString(bytes)
  }

  implicit val convertsLong: ByteConversion[Long] = new ByteConversion[Long] {
    override def convertToBytes(t: Long): Array[Byte] = Longs.toByteArray(t)

    override def convertFromBytes(bytes: Array[Byte]): Long = Longs.fromByteArray(bytes)
  }

  implicit val convertsId: ByteConversion[ErgoId] = new ByteConversion[ErgoId] {
    override def convertToBytes(t: ErgoId): Array[Byte] = t.getBytes

    override def convertFromBytes(bytes: Array[Byte]): ErgoId = new ErgoId(bytes)
  }

  implicit val convertsErgoTree: ByteConversion[Values.ErgoTree] = new ByteConversion[Values.ErgoTree] {
    override def convertToBytes(t: Values.ErgoTree): Array[Byte] = t.bytes

    override def convertFromBytes(bytes: Array[Byte]): Values.ErgoTree = ErgoTreeSerializer.DefaultSerializer.deserializeErgoTree(bytes)
  }


}



