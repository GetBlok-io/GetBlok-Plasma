package io.getblok.getblok_plasma.collections

import io.getblok.getblok_plasma.ByteConversion
import org.ergoplatform.appkit.{ErgoType, ErgoValue}
import sigmastate.eval.Colls
import special.collection.Coll

case class OpResult[V](opt: Option[V])(implicit converter: ByteConversion[V]) {
  lazy val ergoType: ErgoType[Byte] = ErgoType.byteType()

  def ergoValue: ErgoValue[Coll[Byte]] = {
    ErgoValue.of(Colls.fromArray(converter.convertToBytes(opt.getOrElse(throw new NoResultException))), ergoType)
  }

  def toHexString: Option[String] = {
    opt.map(o => converter.toHexString(o).toLowerCase)
  }
}
