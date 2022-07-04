package io.getblok.getblok_plasma.collections

import io.getblok.getblok_plasma.{ByteConversion, PlasmaParameters}
import scorex.crypto.authds.avltree.batch._
import scorex.crypto.hash.{Blake2b256, Digest32}
import sigmastate.AvlTreeFlags

/**
 * Basic key-value mapping of AVL Tree using
 * @param store
 * @tparam K
 * @tparam V
 */
class PlasmaMap[K, V](override val flags: AvlTreeFlags, override val params: PlasmaParameters,
                      initProver: Option[BatchAVLProver[Digest32, Blake2b256.type]] = None)
                     (implicit val convertKey: ByteConversion[K], convertVal: ByteConversion[V]) extends PlasmaBase[K, V]{

  override var prover: BatchAVLProver[Digest32, Blake2b256.type] = {
    initProver.getOrElse(new BatchAVLProver[Digest32, Blake2b256.type](params.keySize, params.valueSizeOpt))
  }

  override def insert(keyVals: (K, V)*): ProvenResult[V] = {
    val response = keyVals
      .map(kv =>
        OpResult (
          prover.performOneOperation(Insert(convertKey.toADKey(kv._1), convertVal.toADVal(kv._2)))
            .map(o => o.map(v => convertVal.convertFromBytes(v)))
        )
      )

    val proof = prover.generateProof()
    ProvenResult(response, Proof(proof))
  }

  override def update(newKeyVals: (K, V)*): ProvenResult[V] = {
    val response = newKeyVals
      .map(kv =>
        OpResult (
          prover.performOneOperation(Update(convertKey.toADKey(kv._1), convertVal.toADVal(kv._2)))
            .map(o => o.map(v => convertVal.convertFromBytes(v)))
        )
      )
    val proof = prover.generateProof()
    ProvenResult(response, Proof(proof))
  }

  override def delete(keys: K*): ProvenResult[V] = {
    val response = keys
      .map(k =>
        OpResult (
          prover.performOneOperation(Remove(convertKey.toADKey(k)))
            .map(o => o.map(v => convertVal.convertFromBytes(v)))
        )
      )
    val proof = prover.generateProof()
    ProvenResult(response, Proof(proof))
  }

  override def lookUp(keys: K*): ProvenResult[V] = {
    val response = keys
      .map(k =>
        OpResult(
          prover.performOneOperation(Lookup(convertKey.toADKey(k)))
            .map(o => o.map(v => convertVal.convertFromBytes(v)))
        )
      )
    val proof = prover.generateProof()
    ProvenResult(response, Proof(proof))
  }

  /**
   * Returns persistent items as a Map
   *
   * @return Return mapping of keys to values
   */
  override def toMap: Map[K, V] = ???
}
