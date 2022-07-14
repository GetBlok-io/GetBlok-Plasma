package io.getblok.getblok_plasma.collections

import io.getblok.getblok_plasma.{ByteConversion, PlasmaParameters}
import org.bouncycastle.util.encoders.Hex
import org.ergoplatform.appkit.{ErgoType, ErgoValue}
import org.ergoplatform.settings.ErgoAlgos.HF
import scorex.crypto.authds.avltree.batch.serialization.BatchAVLProverSerializer
import scorex.crypto.authds.avltree.batch.{BatchAVLProver, PersistentBatchAVLProver, VersionedAVLStorage}
import scorex.crypto.hash.{Blake2b256, Digest32}
import sigmastate.Values.AvlTreeConstant
import sigmastate.serialization.ValueSerializer
import sigmastate.{AvlTreeData, AvlTreeFlags}
import special.sigma.AvlTree

/**
 * Base Trait for all Plasma / AVL Tree based types. Providing easy conversions between on and off chain types.
 */
trait PlasmaBase[K, V] {

  var prover: BatchAVLProver[Digest32, Blake2b256.type]

  val flags:  AvlTreeFlags
  val params: PlasmaParameters
  lazy val ergoType: ErgoType[AvlTree] = ErgoType.avlTreeType()

  def ergoAVLData:    AvlTreeData = AvlTreeData(digest, flags, params.keySize , params.valueSizeOpt)
  def ergoAVLTree: AvlTree     = sigmastate.eval.avlTreeDataToAvlTree(ergoAVLData)
  def serialize:   Array[Byte] = ValueSerializer.serialize(AvlTreeConstant(ergoAVLTree))

  def ergoValue:   ErgoValue[AvlTree] = ErgoValue.of(ergoAVLData)


  def insert(keyVals: (K, V)*): ProvenResult[V]

  def update(newKeyVals: (K, V)*): ProvenResult[V]

  def delete(keys: K*): ProvenResult[V]

  def lookUp(keys: K*): ProvenResult[V]

  def digest = prover.digest

  override def toString: String = Hex.toHexString(digest).toLowerCase

  def getManifest(subTreeDepth: Int = 0): Manifest = {
    implicit val hf: HF = Blake2b256
    val plasmaSerializer = new BatchAVLProverSerializer[Digest32, Blake2b256.type]
    val slicedTree = plasmaSerializer.slice(prover, subTreeDepth)
    val manifest = plasmaSerializer.manifestToBytes(slicedTree._1)
    val subTrees = slicedTree._2.map(plasmaSerializer.subtreeToBytes)
    Manifest(digest, manifest, subTrees)
  }


  /**
   * Returns persistent items as a Map
   * @return Return mapping of keys to values
   */
  def toMap: Map[K, V]
}

