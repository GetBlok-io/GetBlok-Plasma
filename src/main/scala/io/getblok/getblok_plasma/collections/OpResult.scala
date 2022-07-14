package io.getblok.getblok_plasma.collections

import io.getblok.getblok_plasma.ByteConversion
import org.ergoplatform.appkit.{ErgoType, ErgoValue}
import sigmastate.eval.Colls
import special.collection.Coll

import java.lang
import scala.util.Try

case class OpResult[V](tryOp: Try[Option[V]])(implicit converter: ByteConversion[V]) {
  lazy val ergoType: ErgoType[Byte] = ErgoType.byteType()

  def ergoValue: ErgoValue[Coll[Byte]] = {
    ErgoValue.of(Colls.fromArray(converter.convertToBytes(tryOp.getOrElse(throw new NoResultException).getOrElse(throw new NoResultException))), ergoType)
  }

  def toHexString: Option[String] = {
    tryOp.map(o => o.map(r => converter.toHexString(r).toLowerCase)).toOption.flatten
  }
}
