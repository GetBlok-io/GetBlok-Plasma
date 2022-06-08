package io.getblok.getblok_plasma.collections

import io.getblok.getblok_plasma.persistence.SwayDBVersionedStore
import org.bouncycastle.util.encoders.Hex
import org.ergoplatform.appkit.{ErgoType, ErgoValue}
import scorex.crypto.authds.{ADDigest, ADValue}
import scorex.crypto.authds.avltree.batch.{Operation, PersistentBatchAVLProver, VersionedSwayAVLStorage}
import scorex.crypto.hash.{Blake2b256, Digest32}
import sigmastate.Values.AvlTreeConstant
import sigmastate.serialization.ValueSerializer
import sigmastate.{AvlTreeData, AvlTreeFlags}
import special.sigma.{AvlTree, PreHeaderRType}

import java.io.File
import scala.collection.mutable.ArrayBuffer

/**
 * Base Trait for all Plasma / AVL Tree based types. Providing easy conversions between on and off chain types.
 */
trait PlasmaBase[K, V] {

  protected val prover: PersistentBatchAVLProver[Digest32, Blake2b256.type ]
  val storage:  VersionedSwayAVLStorage
  val flags:    AvlTreeFlags

  val ergoType: ErgoType[AvlTree] = ErgoType.avlTreeType()

  def ergoAVLData:    AvlTreeData = AvlTreeData(digest, flags, storage.keySize , storage.valueSizeOpt)
  def ergoAVLTree: AvlTree     = sigmastate.eval.avlTreeDataToAvlTree(ergoAVLData)
  def serialize:   Array[Byte] = ValueSerializer.serialize(AvlTreeConstant(ergoAVLTree))

  def ergoValue:   ErgoValue[AvlTree] = ErgoValue.of(ergoAVLData)


  def insert(keyVals: (K, V)*): ProvenResult[V]

  def update(newKeyVals: (K, V)*): ProvenResult[V]

  def delete(keys: K*): ProvenResult[V]

  def lookUp(keys: K*): ProvenResult[V]

  def digest = prover.digest

  override def toString: String = Hex.toHexString(digest).toLowerCase



  /**
   * Get the key-values currently associated with this Plasma's digest from persistent storage.
   * @return Sequence of Key Values from persistent storage
   */
  def persistentItems: Seq[(K, V)]

  /**
   * Returns persistent items as a Map
   * @return Return mapping of keys to values
   */
  def toMap: Map[K, V]
}
