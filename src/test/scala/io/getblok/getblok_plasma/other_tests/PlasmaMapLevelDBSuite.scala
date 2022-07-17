package io.getblok.getblok_plasma.other_tests

import com.google.common.primitives.{Ints, Longs}
import io.getblok.getblok_plasma.{ByteConversion, PlasmaParameters}
import io.getblok.getblok_plasma.collections.{LocalPlasmaMap, ProvenResult}
import io.getblok.getblok_plasma.other_tests.PlasmaMapLevelDBSuite.{TestLong, convertsTestInt, mockData}
import org.bouncycastle.util.encoders.Hex
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{Address, ErgoId, InputBox, Parameters}
import org.scalatest.funsuite.AnyFunSuite
import scorex.crypto.authds.avltree.batch.VersionedLDBAVLStorage
import scorex.crypto.hash.{Blake2b256, Digest32}
import scorex.db.LDBVersionedStore
import sigmastate.{AvlTreeFlags, Values}

import java.io.File
import scala.collection.immutable
import scala.jdk.CollectionConverters.seqAsJavaListConverter

class PlasmaMapLevelDBSuite extends AnyFunSuite {
  var swayStore: LDBVersionedStore = _
  var avlStorage: VersionedLDBAVLStorage[Digest32] = _
  var localMap: LocalPlasmaMap[Long, TestLong] = _
  var lookUpBox: InputBox = _



  test("Create PlasmaMap") {
    swayStore = new LDBVersionedStore(new File("./level"), 10)
    avlStorage = new VersionedLDBAVLStorage[Digest32](swayStore, PlasmaParameters.default.toNodeParams)(Blake2b256)
    localMap = new LocalPlasmaMap[Long, TestLong](avlStorage, AvlTreeFlags.AllOperationsAllowed, PlasmaParameters.default)
    println(s"Digest ${Hex.toHexString(localMap.digest)}")
  }

  test("Add values") {
    val result = localMap.insert(mockData: _*)

    println(s"Result: ${result}")
   // println(s"Proof: ${result.proof}")
    println(s"Digest: ${Hex.toHexString(localMap.digest)}")
   // println(s"Manifest: ${localMap.toPlasmaMap.makeManifest.toHexString}")
    println(s"Tree height: ${localMap.prover.height}")
  }

//  test("Convert to PlasmaMap"){
//    val map = localMap.toPlasmaMap
//    val manifest = map.getManifest(255)
//    println(s"Digest: ${manifest.digestString}")
//    val manString = manifest.toHexStrings
//    println(s"Manifest: ${manString._1}")
//
//    for(s <- manString._2) println(s"SubTree: ${s}")
//
//    println(s"Num subtrees: ${manString._2.length}")
//  }

  test("lookup id 1") {
        val result = localMap.lookUp(mockData.head._1)
        println(s"Result: ${result}")
  }

  test("Place in box") {
        val result = localMap.lookUp(mockData.head._1)
        println(Longs.toByteArray(mockData.head._1).length)
        println(s"Result: ${result}")
        val box = buildAVLBox(Parameters.OneErg, Longs.toByteArray(mockData.head._1), localMap.ergoValue, result.proof.ergoValue)
        println(box.toJson(true))
  }

    test("Spend box"){
      val randomLongs = for(i <- 1L to 80L) yield i -> TestLong(i.toInt + 1)
      println(randomLongs + " - random int")
      val oldErgoValue = localMap.ergoValue
      val result = localMap.update(randomLongs:_*)
      println(s"Result: ${result}")
      val boxes = buildGetManyAVLBoxes(Parameters.OneErg, randomLongs.map(l => ByteConversion.convertsLong.convertToBytes(l._1) -> convertsTestInt.convertToBytes(l._2)), oldErgoValue, result.proof.ergoValue)
      println(boxes.head.toJson(true))
      ergoClient.execute{
        ctx =>
          val txB = ctx.newTxBuilder()
          val outB = txB.outBoxBuilder()
          val out = outB
            .value(Parameters.OneErg - Parameters.MinFee)
            .contract(new ErgoTreeContract(boxes.head.getErgoTree, ctx.getNetworkType))
            .build()

          val uTx = txB
            .boxesToSpend(boxes.asJava)
            .outputs(out)
            .fee(Parameters.MinFee)
            .sendChangeTo(Address.fromErgoTree(boxes.head.getErgoTree, ctx.getNetworkType).getErgoAddress)
            .build()

          val sTx = dummyProver.sign(uTx)
          println(s"Tx Cost: ${sTx.getCost}")
          println(s"Tx json: ${sTx.toJson(true)}")
          println(s"AVL box size: ${boxes.head.getBytes.length}")
          println(s"Proof box size: ${boxes(1).getBytes.length}")
          println(randomLongs + " - random int")
      }
    }
}

object PlasmaMapLevelDBSuite {

  case class TestLong(i: Long)

  implicit val convertsTestInt: ByteConversion[TestLong] = new ByteConversion[TestLong] {
    override def convertToBytes(t: TestLong): Array[Byte] = Longs.toByteArray(t.i)

    override def convertFromBytes(bytes: Array[Byte]): TestLong = TestLong(Longs.fromByteArray(bytes))
  }

  val mockData: Seq[(Long, TestLong)] = for(i <- 1L to 100000) yield i -> TestLong(i)

  def ergoId(str: String):    ErgoId = ErgoId.create(str)
  def ergoTree(str: String):  Values.ErgoTree = Address.create(str).getErgoAddress.script

}

