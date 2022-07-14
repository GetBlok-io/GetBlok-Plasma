package io.getblok.getblok_plasma.other_tests

import io.getblok.getblok_plasma.PlasmaParameters
import io.getblok.getblok_plasma.collections.{LocalPlasmaMap, PlasmaMap}
import io.getblok.getblok_plasma.other_tests.PlasmaMapLevelDBSuite.{TestLong, convertsTestInt, mockData}
import org.bouncycastle.util.encoders.Hex
import org.ergoplatform.appkit.InputBox
import io.getblok.getblok_plasma.collections.Manifest
import io.getblok.getblok_plasma.other_tests.ManifestSuite.{digestHex, manHex, subtreesHex}
import org.scalatest.funsuite.AnyFunSuite
import scorex.crypto.authds.avltree.batch.VersionedLDBAVLStorage
import scorex.crypto.hash.{Blake2b256, Digest32}
import scorex.db.LDBVersionedStore
import sigmastate.AvlTreeFlags

import java.io.File

class ManifestSuite extends AnyFunSuite {
  var plasma: PlasmaMap[Long, Long] = _

  test("Create PlasmaMap") {
    plasma = new PlasmaMap[Long, Long](AvlTreeFlags.AllOperationsAllowed, PlasmaParameters.default)
    println(s"Digest ${Hex.toHexString(plasma.digest)}")
  }

  test("Add values") {

    plasma.insert(1L -> 1L, 2L -> 2L, 3L -> 3L)
    val manifest = plasma.getManifest()
    println(s"Digest: ${manifest.digestString}")
    val manString = manifest.toHexStrings
    println(s"Manifest: ${manString._1}")

    for(s <- manString._2) println(s"SubTree: ${s}")
  }

  test("Copy tree via manifest"){
    val dupMap = plasma.copy()
    println(s"plasmaDigest: ${plasma.getManifest().digestString}")
    println(s"dupDigest: ${dupMap.getManifest().digestString}")
    require(dupMap.getManifest().digestString == plasma.getManifest().digestString)
  }

  test("Load manifest from string constants"){
    val manifest: Manifest = Manifest.fromHexStrings(digestHex, manHex, subtreesHex)
  }

  test("Verify string loaded manifests have same digest"){
    val manifest: Manifest = Manifest.fromHexStrings(digestHex, manHex, subtreesHex)
    val newMap = new PlasmaMap[Long, Long](AvlTreeFlags.AllOperationsAllowed, PlasmaParameters.default)
    println(s"New map digest: ${newMap.toString}")
    require(newMap.toString != plasma.toString, "Unloaded map had same digest as current map!")
    newMap.loadManifest(manifest)
    println(s"New map digest after loading: ${newMap.toString}")
    println(s"Old map digest: ${plasma.toString}")
    require(newMap.toString == plasma.toString)
  }

}

object ManifestSuite {
  val manHex = "00000020ffffffff0000000201000000000000000002000000000000000000000000000000000000000000000000000000c80100000000000000000100000000000000000000000000000000000000000000000000000041000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000010000000000000000030000000000000000000000000000000000000000000000000000006100000000000000000200000000000000000000000000000000000000000000000000000000000000030000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000003000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000000003000000000000000000000000000000000000000000000000"
  val digestHex = "666f8ca3378c2264f6da9a52bf453a0972151ec60048a3f485d412e3d8a2914102"
  val subtreesHex = Seq(
    "0000000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000",
    "00000000000000000100000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000",
    "00000000000000000200000000000000000000000000000000000000000000000000000000000000030000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000",
    "000000000000000003000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000000003000000000000000000000000000000000000000000000000",
  )
}



