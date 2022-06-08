package io.getblok.getblok_plasma.collections

import io.getblok.getblok_plasma.ByteConversion
import io.getblok.getblok_plasma.persistence.SwayDBVersionedStore
import scorex.crypto.authds.{ADKey, ADValue}
import scorex.crypto.authds.avltree.batch.{BatchAVLProver, Insert, PersistentBatchAVLProver, Remove, Update, VersionedSwayAVLStorage}
import scorex.crypto.hash.{Blake2b256, Digest32}
import sigmastate.AvlTreeFlags
import supertagged.@@

import java.io.File

/**
 * Basic key-value mapping of AVL Tree using
 * @param store
 * @tparam K
 * @tparam V
 */
class PlasmaMap[K, V](store: VersionedSwayAVLStorage, override val flags: AvlTreeFlags)(implicit val convertKey: ByteConversion[K], convertVal: ByteConversion[V]) extends PlasmaBase[K, V]{
  override val storage: VersionedSwayAVLStorage = store

  override protected val prover: PersistentBatchAVLProver[Digest32, Blake2b256.type] = {
    val avlProver = new BatchAVLProver[Digest32, Blake2b256.type](store.keySize, store.valueSizeOpt)
    PersistentBatchAVLProver.create(avlProver, store).getOrElse(throw new ProverCreationException)

  }

  override def insert(keyVals: (K, V)*): ProvenResult[V] = {
    val response = keyVals
      .map(kv =>
        prover.performOneOperation(Insert(convertKey.toADKey(kv._1), convertVal.toADVal(kv._2)))
          .toOption
          .flatten
          .map(o => convertVal.convertFromBytes(o))
      )

    val proof = prover.generateProofAndUpdateStorage()
    ProvenResult(response, Proof(proof))
  }

  override def update(newKeyVals: (K, V)*): ProvenResult[V] = {
    val response = newKeyVals
      .map(kv =>
        prover.performOneOperation(Update(convertKey.toADKey(kv._1), convertVal.toADVal(kv._2)))
          .toOption
          .flatten
          .map(o => convertVal.convertFromBytes(o))
      )
    val proof = prover.generateProofAndUpdateStorage()
    ProvenResult(response, Proof(proof))
  }

  override def delete(keys: K*): ProvenResult[V] = {
    val response = keys
      .map(k =>
        prover.performOneOperation(Remove(convertKey.toADKey(k)))
          .toOption
          .flatten
          .map(o => convertVal.convertFromBytes(o))
      )
    val proof = prover.generateProofAndUpdateStorage()
    ProvenResult(response, Proof(proof))
  }

  override def lookUp(keys: K*): ProvenResult[V] = {
    val response = keys
      .map(k =>
        prover.performOneOperation(Remove(convertKey.toADKey(k)))
          .toOption
          .flatten
          .map(o => convertVal.convertFromBytes(o))
      )
    val proof = prover.generateProofAndUpdateStorage()
    ProvenResult(response, Proof(proof))
  }


  /**
   * Get the key-values currently associated with this PlasmaMap from persistent storage.
   *
   * @return Sequence of Key Values from persistent storage
   */
  override def persistentItems: Seq[(K, V)] = ???

  /**
   * Returns persistent items as a Map
   * @return Return mapping of keys to values
   */
  override def toMap: Map[K, V] = ???
}
