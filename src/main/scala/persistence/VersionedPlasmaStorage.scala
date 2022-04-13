package io.getblok.getblok_plasma
package persistence

import com.google.common.primitives.Ints
import io.getblok.getblok_plasma.persistence.VersionedPlasmaStorage.{InternalNodePrefix, LeafPrefix}
import scorex.crypto.authds.avltree.batch._
import scorex.crypto.authds.{ADDigest, ADKey, ADValue, Balance}
import scorex.crypto.hash.{Blake2b256, Digest32}

import scala.util.Try

/**
 * Storage for plasma that may be generated from a VersionedPlasmaMap. Allows for implementation of
 * some underlying PersistentBatchProver. Much of the code was recycled from implementation using LDB on Ergo.
 * Uses Digest32 for versions, equal values for KeySize and LabelSize, and Blake2b256 hashing
 * @param plasmaMap Versioned plasma map
 * @param plasmaParams size parameters for plasma
 */
class VersionedPlasmaStorage(plasmaMap: VersionedPlasmaMap, plasmaParams: PlasmaParameters) extends VersionedAVLStorage[Digest32]{
  val keySize: Int = plasmaParams.keySize
  val valueSizeOpt: Option[Int] = plasmaParams.valueSizeOpt
  private val fixedSizeValueMode = valueSizeOpt.isDefined

  private val TopNodeKey: Array[Byte] = Array.fill(keySize)(123: Byte)
  private val TopNodeHeight: Array[Byte] = Array.fill(keySize)(124: Byte)

  override def update[K <: Array[Byte], V <: Array[Byte]](batchProver: BatchAVLProver[Digest32, _], additionalData: Seq[(K, V)]): Try[Unit] = {
    ???
  }

  override def rollback(version: ADDigest): Try[(ProverNodes[Digest32], Int)] = ???

  override def version: Option[ADDigest] = ???

  override def rollbackVersions: Iterable[ADDigest] = ???

  private def nodeKey(node: ProverNodes[Digest32]): Array[Byte] = node.label

  private def toBytes(node: ProverNodes[Digest32]): Array[Byte] = node match {
    case n: InternalProverNode[Digest32] => InternalNodePrefix +: n.balance +: (n.key ++ n.left.label ++ n.right.label)
    case n: ProverLeaf[Digest32] =>
      if (fixedSizeValueMode){
        LeafPrefix +: (n.key ++ n.value ++ n.nextLeafKey)
      } else {
        LeafPrefix +: (n.key ++ Ints.toByteArray(n.value.length) ++ n.value ++ n.nextLeafKey)
      }
  }
}

object VersionedPlasmaStorage {
  val InternalNodePrefix: Byte = 0: Byte
  val LeafPrefix: Byte = 1: Byte

  implicit val hf = Blake2b256
  def fetch(key: ADKey)(implicit store: VersionedPlasmaMap,
                        plasmaParams: PlasmaParameters): ProverNodes[Digest32] = {
    val keySize = plasmaParams.keySize
    val valueSizeOpt = plasmaParams.valueSizeOpt

    val bytes = store(key)

    bytes.head match {
      case InternalNodePrefix =>
        val balance = Balance @@ bytes.slice(1, 2).head
        val key = ADKey @@ bytes.slice(2, 2 + keySize)
        val leftKey = ADKey @@ bytes.slice(2 + keySize, 2 + (keySize * 2))
        val rightKey = ADKey @@ bytes.slice(2 + (2 * keySize), 2 + (3 * keySize))

        val n = new ProxyInternalProverNode(key, leftKey, rightKey, balance)
        n.isNew = false
        n
      case LeafPrefix =>
        val key = ADKey @@ bytes.slice(1, 1 + keySize)
        val (value, nextLeafKey) = if (valueSizeOpt.isDefined) {
          val valueSize = valueSizeOpt.get
          val value = ADValue @@ bytes.slice(1 + keySize, 1 + keySize + valueSize)
          val nextLeafKey = ADKey @@ bytes.slice(1 + keySize + valueSize, 1 + (2 * keySize) + valueSize)
          value -> nextLeafKey
        } else {
          val valueSize = Ints.fromByteArray(bytes.slice(1 + keySize, 1 + keySize + 4))
          val value = ADValue @@ bytes.slice(1 + keySize + 4, 1 + keySize + 4 + valueSize)
          val nextLeafKey = ADKey @@ bytes.slice(1 + keySize + 4 + valueSize, 1 + (2 * keySize) + 4 + valueSize)
          value -> nextLeafKey
        }
        val l = new ProverLeaf(key, value, nextLeafKey)
        l.isNew = false
        l
    }
  }
}
