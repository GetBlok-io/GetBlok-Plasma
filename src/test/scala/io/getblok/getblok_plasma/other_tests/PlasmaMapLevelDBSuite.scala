package io.getblok.getblok_plasma.other_tests

import com.google.common.primitives.{Ints, Longs}
import io.getblok.getblok_plasma.{ByteConversion, PlasmaParameters}
import io.getblok.getblok_plasma.collections.{PlasmaMap, ProvenResult}
import io.getblok.getblok_plasma.other_tests.PlasmaMapLevelDBSuite.mockData
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
  var plasmaMap: PlasmaMap[Long, Array[Byte]] = _
  var lookUpBox: InputBox = _
  test("Create PlasmaMap") {
    swayStore = new LDBVersionedStore(new File("./level"), 10)
    avlStorage = new VersionedLDBAVLStorage[Digest32](swayStore, PlasmaParameters.default.toNodeParams)(Blake2b256)
    plasmaMap = new PlasmaMap[Long, Array[Byte]](avlStorage, AvlTreeFlags.AllOperationsAllowed, PlasmaParameters.default)
    println(s"Digest ${Hex.toHexString(plasmaMap.digest)}")
  }

  test("Add values") {
    val result: ProvenResult[Array[Byte]] = plasmaMap.insert(mockData: _*)
    println(s"Result: ${result}")
    println(s"Proof: ${result.proof}")
    println(s"Digest: ${Hex.toHexString(plasmaMap.digest)}")
    println(s"Manifest: ${plasmaMap.manifest.toHexString}")
  }

  test("lookup id 1") {
        val result = plasmaMap.lookUp(mockData.head._1)
        println(s"Result: ${result}")
  }

  test("Place in box") {
        val result = plasmaMap.lookUp(mockData.head._1)
        println(Longs.toByteArray(mockData.head._1).length)
        println(s"Result: ${result}")
        val box = buildAVLBox(Parameters.OneErg, Longs.toByteArray(mockData.head._1), plasmaMap.ergoValue, result.proof.ergoValue)
        println(box.toJson(true))
  }

    test("Spend box"){
      val randomLongs = for(i <- 1L to 80L) yield i
      println(randomLongs + " - random int")
      val result = plasmaMap.lookUp(randomLongs:_*)
      println(s"Result: ${result}")
      val boxes = buildGetManyAVLBoxes(Parameters.OneErg, randomLongs.map(l => ByteConversion.convertsLong.convertToBytes(l)), plasmaMap.ergoValue, result.proof.ergoValue)
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
  val mockData: Seq[(Long, Array[Byte])] = for(i <- 1L to 100000L) yield i -> Ints.toByteArray(i.toInt)

  def ergoId(str: String):    ErgoId = ErgoId.create(str)
  def ergoTree(str: String):  Values.ErgoTree = Address.create(str).getErgoAddress.script

}

