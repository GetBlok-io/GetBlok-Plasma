package io.getblok.getblok_plasma.collections

import io.getblok.getblok_plasma.ByteConversion
import org.ergoplatform.appkit.JavaHelpers.JByteRType
import org.ergoplatform.appkit.{ErgoType, ErgoValue, Iso}
import sigmastate.eval.Colls
import special.collection.Coll

import java.lang
import scala.util.Try

case class OpResult[V](tryOp: Try[Option[V]])(implicit converter: ByteConversion[V]) {
  lazy val ergoType: ErgoType[java.lang.Byte] = ErgoType.byteType()

  def ergoValue: ErgoValue[Coll[java.lang.Byte]] = {
    ErgoValue.of(Colls.fromArray(
      converter.convertToBytes(tryOp.getOrElse(throw new NoResultException).getOrElse(throw new NoResultException)))
      .map(Iso.jbyteToByte.from)
      , ergoType)
  }

  def toHexString: Option[String] = {
    tryOp.map(o => o.map(r => converter.toHexString(r).toLowerCase)).toOption.flatten
  }
}
