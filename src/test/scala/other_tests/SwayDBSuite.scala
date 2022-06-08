package other_tests

import org.scalatest.funsuite.AnyFunSuite
import boopickle.Default._
import com.google.common.primitives.Longs
import io.getblok.getblok_plasma.PlasmaParameters
import io.getblok.getblok_plasma.other_tests.{buildUserBox, randomLongKey}
import io.getblok.getblok_plasma.persistence.SwayDBVersionedStore
import org.slf4j.{Logger, LoggerFactory}
import scorex.crypto.authds.{ADKey, ADValue}
import scorex.crypto.authds.avltree.batch.serialization.BatchAVLProverSerializer
import scorex.crypto.authds.avltree.batch.{BatchAVLProver, Insert, PersistentBatchAVLProver, VersionedSwayAVLStorage}
import scorex.crypto.hash.{Blake2b256, Digest32}
import supertagged.@@

import java.io.File
class SwayDBSuite extends AnyFunSuite{
  val logger: Logger = LoggerFactory.getLogger("PlasmaSuite")
  val swayDBVersionedStore = new SwayDBVersionedStore(new File("C:\\Users\\Kirat\\IdeaProjects\\GetBlok-Plasma\\sway"))
  val versionedPlasma = new VersionedSwayAVLStorage(swayDBVersionedStore, PlasmaParameters(32, None))

  val avlProver = new BatchAVLProver[Digest32, Blake2b256.type](32 , None)
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
  swayDBVersionedStore.close()

  def toHexString(arr: Array[Byte]) = {
    BigInt(arr).toString(16)
  }
}
