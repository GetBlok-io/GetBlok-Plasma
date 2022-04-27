package scorex.crypto.authds.avltree.batch

import com.google.common.primitives.Longs
import io.getblok.getblok_plasma.sway.SwayDBVersionedStore
import io.getblok.getblok_plasma.sway.Types.{PlasmaKey, PlasmaVal, VersionedDigest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scorex.crypto.authds.avltree.batch.helpers.TestHelper
import scorex.crypto.hash.Blake2b256
import scorex.db.LDBVersionedStore

import scala.collection.mutable.ArrayBuffer

class SwayDBVersionedStoreSpec extends AnyPropSpec
  with ScalaCheckPropertyChecks
  with Matchers
  with TestHelper {

  override protected val KL = 32
  override protected val VL = 8
  override protected val LL = 32

  val storeTest: SwayDBVersionedStore => Unit = { store =>
    store.open()
    var version = store.lastVersion
    val keys: ArrayBuffer[(Array[Byte], Array[Byte])] = ArrayBuffer()
    forAll { b: Array[Byte] =>
      val pair = (Blake2b256(0.toByte +: version.map(v => v.digest.head).getOrElse(0: Byte) +: b),
        Blake2b256(version.map(_.digest.head).getOrElse(0: Byte) +: b))
      keys += pair
      // Versioned storage is implemented differently, so lets just place in a random long
      val nextVersion = VersionedDigest(Longs.toByteArray((Math.random() * 1000000L).toLong), version.map(v => v.versionNum).getOrElse(0L) + 1)
      store.update(nextVersion.digest, Seq(), Seq(pair).map(p => PlasmaKey(p._1) -> PlasmaVal(p._2))).get

      if (version.isDefined) {
        store.rollbackTo(version.get.digest)
        store.update(nextVersion.digest, Seq(), Seq(pair).map(p => PlasmaKey(p._1) -> PlasmaVal(p._2))).get
      }
      version = store.getVersion(nextVersion.digest)
      keys.foreach(k => store(PlasmaKey(k._1)).value.sameElements(k._2) shouldBe true)
    }
  }

  property("SwayDBVersionedStore") { storeTest(createSwayStore()) }

}
