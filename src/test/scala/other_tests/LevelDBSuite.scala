package other_tests

import io.getblok.getblok_plasma.PlasmaParameters
import io.getblok.getblok_plasma.other_tests.{buildUserBox, randomLongKey}
import scorex.crypto.authds.ADKey
import supertagged.@@
//import io.getblok.getblok_plasma.sway.SwayDBVersionedStore
import org.scalatest.funsuite.AnyFunSuite
import org.slf4j.{Logger, LoggerFactory}
import scorex.crypto.authds.ADValue
import scorex.crypto.authds.avltree.batch.{BatchAVLProver, Insert, NodeParameters, PersistentBatchAVLProver, VersionedLDBAVLStorage}
import scorex.crypto.hash.{Blake2b256, Digest32}
import scorex.crypto.authds.avltree.batch
import scorex.db.LDBVersionedStore

import java.io.File

class LevelDBSuite extends AnyFunSuite{
  val logger: Logger = LoggerFactory.getLogger("PlasmaSuite")
  val ldbVersionedStore = new LDBVersionedStore(new File("C:\\Users\\Kirat\\IdeaProjects\\GetBlok-Plasma\\ldb"), 1)
  val versionedPlasma = new VersionedLDBAVLStorage[Digest32](ldbVersionedStore, NodeParameters(32, None, 32))(Blake2b256)

  val avlProver = new BatchAVLProver[Digest32, Blake2b256.type](32, None)
  val persistentBatchAVLProver = PersistentBatchAVLProver.create(avlProver, versionedPlasma)

  if(persistentBatchAVLProver.isSuccess){
    logger.info(s"Current digest: ${toHexString(persistentBatchAVLProver.get.digest)}")
    logger.info(s"Current Tree: ${persistentBatchAVLProver.get.prover().toString()}")
    val randomLong = randomLongKey
    val randomBox = buildUserBox(100000000000L).getId.toString
    logger.info(s"Random Box Id: ${randomBox}")
    val randomId = ADValue @@ buildUserBox(100000000000L).getId.getBytes
    logger.info(s"Long: $randomLong")
    logger.info("Long Hex: " + toHexString(randomLong))
    logger.info("Box Id Hex: " + toHexString(randomId))
    val doOp = persistentBatchAVLProver.get.performOneOperation(Insert(ADKey @@ (randomLong ++ Array.fill(24)(0.toByte)), randomId))
    if(doOp.isSuccess) {
      val proof = persistentBatchAVLProver.get.generateProofAndUpdateStorage()
      logger.info(s"Current proof: ${toHexString(proof)}")
    }
    logger.info("Current digest : " + toHexString(versionedPlasma.version.get))

    logger.info(s"New digest: ${toHexString(persistentBatchAVLProver.get.prover().digest)}")
    logger.info(s"New tree: ${persistentBatchAVLProver.get.prover().toString()}")
  }else{
    logger.error("There was an error", persistentBatchAVLProver.failed.get)
  }


  def toHexString(arr: Array[Byte]) = {
    BigInt(arr).toString(16)
  }
}
