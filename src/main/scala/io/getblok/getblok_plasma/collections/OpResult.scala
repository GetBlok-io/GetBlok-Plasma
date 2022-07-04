package io.getblok.getblok_plasma.collections

import io.getblok.getblok_plasma.ByteConversion
import org.ergoplatform.appkit.{ErgoType, ErgoValue}
import sigmastate.eval.Colls
import special.collection.Coll

import scala.util.Try

case class OpResult[V](opt: Try[Option[V]])(implicit converter: ByteConversion[V]) {
  lazy val ergoType: ErgoType[Byte] = ErgoType.byteType()

  def ergoValue: ErgoValue[Coll[Byte]] = {
    ErgoValue.of(Colls.fromArray(converter.convertToBytes(opt.getOrElse(throw new NoResultException).getOrElse(throw new NoResultException))), ergoType)
  }

  def toHexString: Option[String] = {
    opt.map(o => o.map(r => converter.toHexString(r).toLowerCase)).toOption.flatten
  }
}
