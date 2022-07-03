package io.getblok.getblok_plasma.other_tests

import com.google.common.primitives.Longs
import org.ergoplatform.appkit.{ErgoValue, Parameters, SecretString}
import org.ergoplatform.settings.ErgoAlgos.HF
import org.scalatest.funsuite.AnyFunSuite
import org.slf4j.{Logger, LoggerFactory}
import scorex.crypto.authds.avltree.batch.{BatchAVLProver, Insert}
import scorex.crypto.authds.avltree.batch.serialization.BatchAVLProverSerializer
import scorex.crypto.authds.{ADDigest, ADKey, ADValue}
import scorex.crypto.hash.{Blake2b256, Digest32}
import sigmastate.Values.AvlTreeConstant
import sigmastate.serialization.ValueSerializer
import sigmastate.{AvlTreeData, AvlTreeFlags}
import special.sigma.AvlTree
import supertagged.@@

import scala.util.Try

class PlasmaSuite extends AnyFunSuite{

  val logger: Logger = LoggerFactory.getLogger("PlasmaSuite")
  val longNumBytes: Int = Longs.toByteArray(0).length
  logger.info(s"Long num Bytes: ${longNumBytes}")
  val initLongKey = Longs.toByteArray(0)
  val secondLongKey = Longs.toByteArray(Parameters.OneErg)
  val keyLength = 4
  val initKeySize = 0
  var keysAdded: Array[Array[Byte] @@ ADKey.Tag] = Array()

  case class SerializedPlasma(lastDigest: Array[Byte], manifest: Array[Byte], subTrees: Seq[Array[Byte]]) {
    override def toString: String = {
      "SerializedPlasma("+ toHexString(manifest) +", " + subTrees.map(toHexString).mkString("", ", ", "") + ")"
    }
  }

  case class Plasma(digest: Array[Byte], keyLength: Int, valueLengthOpt: Option[Int] = None,
                    lastProver: Option[BatchAVLProver[Digest32, Blake2b256.type]] = None) {
    def this(avlTree: AvlTree) = this(avlTree.digest.toArray, avlTree.keyLength, avlTree.valueLengthOpt)

    def getData: AvlTreeData = AvlTreeData(ADDigest @@ digest, AvlTreeFlags.AllOperationsAllowed, keyLength, valueLengthOpt)
    val value: AvlTree = sigmastate.eval.avlTreeDataToAvlTree(getData)

    lazy val serialize: Array[Byte] = ValueSerializer.serialize(AvlTreeConstant(value))
    val typeName: String = "AvlTree"
    override lazy val toString = BigInt(serialize).toString(16)
    def getErgoValue: ErgoValue[AvlTree] = ErgoValue.of(getData)

    def serializeOffChain: SerializedPlasma = {
      implicit val hf: HF = Blake2b256
      val plasmaSerializer = new BatchAVLProverSerializer[Digest32, Blake2b256.type]
      val slicedTree = plasmaSerializer.slice(lastProver.get, 0)
      val manifest = plasmaSerializer.manifestToBytes(slicedTree._1)
      val subTrees = slicedTree._2.map(plasmaSerializer.subtreeToBytes)
      SerializedPlasma(lastProver.get.digest, manifest, subTrees)
    }
  }

  object Plasma {
    def serializedProver(serializedPlasma: SerializedPlasma): Try[BatchAVLProver[Digest32, HF]] = {
      implicit val hf: HF = Blake2b256
      val plamaSerializer = new BatchAVLProverSerializer[Digest32, Blake2b256.type]

      val manifest = plamaSerializer.manifestFromBytes(serializedPlasma.manifest)
      val subTrees = serializedPlasma.subTrees.map(t => plamaSerializer.subtreeFromBytes(t, keyLength))
      val newTreeProver = plamaSerializer.combine(manifest.get, subTrees.map(_.get))
      newTreeProver
    }

    def fromSerialized(serializedPlasma: SerializedPlasma): Plasma = {
      implicit val hf: HF = Blake2b256
      val plamaSerializer = new BatchAVLProverSerializer[Digest32, Blake2b256.type]

      val manifest = plamaSerializer.manifestFromBytes(serializedPlasma.manifest)
      val subTrees = serializedPlasma.subTrees.map(t => plamaSerializer.subtreeFromBytes(t, keyLength))
      val newTreeProver = plamaSerializer.combine(manifest.get, subTrees.map(_.get))
      fromProver(newTreeProver.get)
    }


    def fromProver(batchAVLProver: BatchAVLProver[Digest32, Blake2b256.type ]): Plasma = {

      Plasma(batchAVLProver.digest, batchAVLProver.keyLength, batchAVLProver.valueLengthOpt, Some(batchAVLProver))
    }
//    def fromVerifier(batchAVLVerifier: BatchAVLVerifier[Digest32, Blake2b256.type ], proof: SerializedAdProof): Plasma = {
//      Plasma(batchAVLVerifier.digest, batchAVLVerifier.keyLength, batchAVLVerifier.valueLengthOpt, None, Some(proof))
//    }

  }

  def randomInsert(plasma: Plasma) = {
    assert(plasma.lastProver.isDefined)

    val prover = plasma.lastProver.get
    logger.info(s"Current prover: ${prover.toString()}")
    val box = buildUserBox(Parameters.OneErg)
    val valueAdded = ADValue @@ box.getId.getBytes
    val nextKey = randomLongKey
    logger.info(s"Adding new key ${toHexString(nextKey)} with value ${toHexString(valueAdded)}")
    val checkOp = prover.performOneOperation(Insert(randomLongKey, valueAdded))
    if(checkOp.isSuccess){
      logger.info("Successfully added key!")
      keysAdded = keysAdded ++ Array(nextKey)
    }
    logger.info(s"Current prover: ${prover.toString()}")
    Plasma.fromProver(prover)
  }



  test("Try one Operation")  {
    val initPlasma = createAVLTree
    val nextPlasma = randomInsert(initPlasma)

    logger.info("Initial Plasma: " + initPlasma.toString)
    logger.info("Next Plasma: " + nextPlasma.toString)
    logger.info("Next SerializedPlasma " + nextPlasma.serializeOffChain.toString)

  }
  test("Serialize Plasma") {
    val initPlasma = createAVLTree
    val nextPlasma = randomInsert(initPlasma)
    val serializedPlasma = nextPlasma.serializeOffChain
    logger.info("Plasma after op: " + nextPlasma.toString)
    logger.info("SerializedPlasma after insert: " + serializedPlasma.toString)
    initPlasma.lastProver.get

    val loadPlasma = Plasma.fromSerialized(serializedPlasma)
    logger.info("Plasma after loading: " + loadPlasma.toString)

  }


  def createAVLTree = {
    logger.info("Hello!")

    val newBox = buildUserBox(1000000000L)
    val initKey: Array[Byte] @@ ADKey.Tag = ADKey @@ secondLongKey.take(8)
    val initValue = ADValue @@ newBox.getId.getBytes

    logger.info("initKey: " + toHexString(secondLongKey))
    val avlProver = new BatchAVLProver[Digest32, Blake2b256.type](longNumBytes, None)
    logger.info("Initial empty digest: " + toHexString(avlProver.digest))
    logger.info(s"Digest length: ${avlProver.digest.length}")
    val plasma = Plasma.fromProver(avlProver)
    logger.info(plasma.serializeOffChain.toString)
    plasma
  }

  test("Try multiple ops") {
    logger.info("Long num bytes: " + longNumBytes)
    logger.info("Init Key: " + initLongKey.mkString("Array(", ", ", ")"))
    logger.info("Init Key: " + toHexString(initLongKey))
    logger.info("Second Key: " + secondLongKey.mkString("Array(", ", ", ")"))
    logger.info("Second Key: " + toHexString(secondLongKey))

    val newBox = buildUserBox(1000000000L)
    val initKey: Array[Byte] @@ ADKey.Tag = ADKey @@ initLongKey.take(longNumBytes)
    val initValue = ADValue @@ newBox.getId.getBytes

    val avlProver = new BatchAVLProver[Digest32, Blake2b256.type](longNumBytes, None)
    val opProofs = avlProver.generateProofForOperations(Seq(Insert(initKey, initValue)))
    if(opProofs.isSuccess){
      logger.info("Successful operations!")
      logger.info("Proof: " + toHexString(opProofs.get._1))
      logger.info("New Digest: " + toHexString(opProofs.get._1))
    }
  }



  def lookupKey = {
    val plasma = createAVLTree
  }

  def toHexString(arr: Array[Byte]) = {
    BigInt(arr).toString(16)
  }

  test("Create AVL Tree") {
    createAVLTree
  }


  test("Wallet") {
    ergoClient.execute{
      ctx =>
        val prover1 = ctx.newProverBuilder().withMnemonic(SecretString.create("hello"), SecretString.create("")).build()
        val prover2 = ctx.newProverBuilder().withMnemonic(SecretString.create("hello2"), SecretString.create("")).build()
        logger.info(prover1.getAddress.toString)
        logger.info(prover2.getAddress.toString)

    }
  }
}
