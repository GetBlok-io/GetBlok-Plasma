package scorex.crypto.authds.avltree.batch

import com.google.common.primitives.Ints
import io.getblok.getblok_plasma.PlasmaParameters
import io.getblok.getblok_plasma.persistence.PlasmaProxyInternalProverNode
import io.getblok.getblok_plasma.sway.SwayDBVersionedStore
import io.getblok.getblok_plasma.sway.Types.{PlasmaKey, PlasmaVal}
import org.slf4j.{Logger, LoggerFactory}
import scorex.crypto.authds.avltree.batch.VersionedPlasmaStorage.{InternalNodePrefix, LeafPrefix, toHex}
import scorex.crypto.authds.avltree.batch._
import scorex.crypto.authds.{ADDigest, ADKey, ADValue, Balance}
import scorex.crypto.hash.{Blake2b256, Digest32}
import scorex.util.encode.Base58
import swaydb.Glass

import scala.util.{Failure, Try}

/**
 * Storage for plasma that may be generated from a VersionedPlasmaMap. Allows for implementation of
 * some underlying PersistentBatchProver. Much of the code was recycled from implementation using LDB on Ergo.
 * Uses Digest32 for versions, equal values for KeySize and LabelSize, and Blake2b256 hashing
 * @param plasmaMap Versioned plasma map
 * @param plasmaParams size parameters for plasma
 */
class VersionedPlasmaStorage(store: SwayDBVersionedStore, plasmaParams: PlasmaParameters) extends VersionedAVLStorage[Digest32]{
  val keySize: Int = plasmaParams.keySize
  val valueSizeOpt: Option[Int] = plasmaParams.valueSizeOpt
  private val fixedSizeValueMode = valueSizeOpt.isDefined
  private val logger: Logger = LoggerFactory.getLogger("VersionedPlasmaStorage")
  private val TopNodeKey: Array[Byte] = Array.fill(keySize)(123: Byte)
  private val TopNodeHeight: Array[Byte] = Array.fill(keySize)(124: Byte)
  store.open()
  override def update[K <: Array[Byte], V <: Array[Byte]](batchProver: BatchAVLProver[Digest32, _], additionalData: Seq[(K, V)]): Try[Unit] = {
    val digestWrapper = batchProver.digest
    val indexes = Seq(TopNodeKey -> nodeKey(batchProver.topNode), TopNodeHeight -> Ints.toByteArray(batchProver.rootNodeHeight).toArray)
    val toInsert = serializedVisitedNodes(batchProver.topNode, isTop = true)
    val toRemove = batchProver.removedNodes().map(rn => rn.label).map(a => PlasmaKey(a))
    val toUpdate = indexes ++ toInsert
    val toUpdateWithWrapped = (toUpdate ++ additionalData).map(a => (PlasmaKey(a._1) -> PlasmaVal(a._2)))
    store.update(digestWrapper, toRemove, toUpdateWithWrapped)
  }

  override def rollback(version: ADDigest): Try[(ProverNodes[Digest32], Int)] = {
    Try {
      store.rollbackTo(version)
      logger.info(s"TopNodeKey: ${toHex(TopNodeKey)}")
      val top = VersionedPlasmaStorage.fetch(ADKey @@ store.get(PlasmaKey(TopNodeKey)).get.get.value)(store, plasmaParams)
      val topHeight = Ints.fromByteArray(store.get(PlasmaKey(TopNodeHeight)).get.get.value)

      top -> topHeight
    }.recoverWith { case e =>

      Failure(e)
    }
  }

  override def version: Option[ADDigest] = store.lastVersion.map(v => ADDigest @@ v.mapKey.digest)

  override def rollbackVersions: Iterable[ADDigest] = Seq(version.get).reverse

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
  private def serializedVisitedNodes(node: ProverNodes[Digest32],
                                     isTop: Boolean): Seq[(Array[Byte], Array[Byte])] = {
    // Should always serialize top node. It may not be new if it is the creation of the tree
    if (node.isNew || isTop) {
      val pair: (Array[Byte], Array[Byte]) = (nodeKey(node), toBytes(node))
      node match {
        case n: InternalProverNode[Digest32] =>
          val leftSubtree = serializedVisitedNodes(n.left, isTop = false)
          val rightSubtree = serializedVisitedNodes(n.right, isTop = false)
          pair +: (leftSubtree ++ rightSubtree)
        case _: ProverLeaf[Digest32] => Seq(pair)
      }
    } else {
      Seq.empty
    }
  }
}

object VersionedPlasmaStorage {
  val InternalNodePrefix: Byte = 0: Byte
  val LeafPrefix: Byte = 1: Byte
  private val logger: Logger = LoggerFactory.getLogger("VersionedPlasmaStorage")
  implicit val hf = Blake2b256

  def toHex(arr: Array[Byte]) = BigInt(arr).toString(16)


  def fetch(key: ADKey)(implicit store: SwayDBVersionedStore,
                        plasmaParams: PlasmaParameters): ProverNodes[Digest32] = {
    val keySize = plasmaParams.keySize
    val labelSize = plasmaParams.keySize
    val valueSizeOpt = plasmaParams.valueSizeOpt
    logger.info(s"Searching for key ${toHex(key)} with length ${key.length}")
    val bytesOpt = store.get(PlasmaKey(key)).get
      val bytes = bytesOpt.get.value
      bytes.head match {
        case InternalNodePrefix =>
          val balance = Balance @@ bytes.slice(1, 2).head
          val key = ADKey @@ bytes.slice(2, 2 + keySize)
          val leftKey = ADKey @@ bytes.slice(2 + keySize, 2 + keySize + labelSize)
          val rightKey = ADKey @@ bytes.slice(2 + keySize + labelSize, 2 + keySize + (2 * labelSize))

          val n = new PlasmaProxyInternalProverNode(key, leftKey, rightKey, balance)
          n.isNew = false
          n
        case LeafPrefix =>
          val key = ADKey @@ bytes.slice(1, 1 + keySize)
          val (value, nextLeafKey) = if (plasmaParams.valueSizeOpt.isDefined) {
            val valueSize = plasmaParams.valueSizeOpt.get
            val value = ADValue @@ bytes.slice(1 + keySize, 1 + keySize + valueSize)
            val nextLeafKey = ADKey @@ bytes.slice(1 + keySize + valueSize, 1 + (2 * keySize) + valueSize)
            value -> nextLeafKey
          } else {
            val valueSize = Ints.fromByteArray(bytes.slice(1 + keySize, 1 + keySize + 4))
            val value = ADValue @@ bytes.slice(1 + keySize + 4, 1 + keySize + 4 + valueSize)
            val nextLeafKey = ADKey @@ bytes.slice(1 + keySize + 4 + valueSize, 1 + (2 * keySize) + 4 + valueSize)
            value -> nextLeafKey
          }
          val l = new ProverLeaf[Digest32](key, value, nextLeafKey)
          l.isNew = false
          l
      }
  }
}
