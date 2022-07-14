package io.getblok.getblok_plasma.collections

import io.getblok.getblok_plasma.{ByteConversion, PlasmaParameters}
import org.ergoplatform.settings.ErgoAlgos.HF
import scorex.crypto.authds.avltree.batch._
import scorex.crypto.authds.avltree.batch.serialization.BatchAVLProverSerializer
import scorex.crypto.hash.{Blake2b256, Digest32}
import sigmastate.AvlTreeFlags

/**
 * Basic key-value mapping with an underlying AVL Tree. PlasmaMaps represent temporary (non-persistent) AVL Trees.
 * @param flags AvlTreeFlags associated with this PlasmaMap
 * @tparam K
 * @tparam V
 */
class PlasmaMap[K, V](override val flags: AvlTreeFlags, override val params: PlasmaParameters,
                      initProver: Option[BatchAVLProver[Digest32, Blake2b256.type]] = None)
                     (implicit val convertKey: ByteConversion[K], convertVal: ByteConversion[V]) extends PlasmaBase[K, V]{

  override var prover: BatchAVLProver[Digest32, Blake2b256.type] = {
    initProver.getOrElse(new BatchAVLProver[Digest32, Blake2b256.type](params.keySize, params.valueSizeOpt))
  }
  /**
   * Insert new key-value pairs into the PlasmaMap.
   * @param keyVals New, unique key-value pairs that will be inserted into the PlasmaMap
   * @return ProvenResult holding returned OpResults and associated Proof
   */
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
  /**
   * Update key-value pairs existing in the PlasmaMap.
   * @param newKeyVals Key-value pairs that will replace existing pairs in the PlasmaMap
   * @return ProvenResult holding returned OpResults and associated Proof
   */
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
  /**
   * Delete key-value pairs existing in the PlasmaMap
   * @param keys Keys to remove from the underlying PlasmaMap
   * @return ProvenResult holding returned OpResults and associated Proof
   */
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

  /**
   * Look up values existing in the PlasmaMap
   * @param keys Keys to search for in the underlying PlasmaMap
   * @return ProvenResult holding returned OpResults and associated Proof
   */
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
   * Load this PlasmaMap with the key-values associated with the given Manifest
   * @return This PlasmaMap, with its underlying AVL Tree modified and reconstructed using the Manifest
   */
  def loadManifest(manifest: Manifest): PlasmaMap[K, V] = {
    implicit val hf: HF = Blake2b256
    val plamaSerializer = new BatchAVLProverSerializer[Digest32, Blake2b256.type]

    val treeManifest = plamaSerializer.manifestFromBytes(manifest.bytes)
    val subTrees = manifest.subTrees.map(t => plamaSerializer.subtreeFromBytes(t, params.keySize))

    prover = plamaSerializer.combine(treeManifest.get, subTrees.map(_.get)).getOrElse(throw new ProverCreationException)
    this
  }

  /**
   * Copy this PlasmaMap into a new PlasmaMap object using the underlying tree manifest
   * @param optFlags Optional flags to set on the new PlasmaMap. Defaults to copying the flags from the current PlasmaMap.
   * @param optParams Optional parameters to set on the new PlasmaMap. Defaults to copying the parameters from the current PlasmaMap.
   * @return A new PlasmaMap with the exact same key-values and digest as the current PlasmaMap
   */
  def copy(optFlags: Option[AvlTreeFlags] = None, optParams: Option[PlasmaParameters] = None): PlasmaMap[K, V] = {
    val treeCopy = new PlasmaMap[K, V](optFlags.getOrElse(flags), optParams.getOrElse(params))
    treeCopy.loadManifest(getManifest())
  }

  /**
   * Returns persistent items as a Map
   *
   * @return Return mapping of keys to values
   */
  override def toMap: Map[K, V] = ???
}
