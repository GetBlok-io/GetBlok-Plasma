package other_tests

import io.getblok.getblok_plasma.PlasmaParameters
import io.getblok.getblok_plasma.collections.PlasmaMap
import io.getblok.getblok_plasma.persistence.SwayDBVersionedStore
import org.ergoplatform.appkit.{Address, ErgoId}
import org.scalatest.funsuite.AnyFunSuite
import other_tests.PlasmaMapSuite.mockData
import scorex.crypto.authds.avltree.batch.VersionedSwayAVLStorage
import sigmastate.{AvlTreeFlags, Values}

import java.io.File

class PlasmaMapSuite extends AnyFunSuite{
  var swayStore:  SwayDBVersionedStore                  = _
  var avlStorage: VersionedSwayAVLStorage               = _
  var plasmaMap:  PlasmaMap[ErgoId, Values.ErgoTree]    = _
  test("Create PlasmaMap"){
     swayStore = new SwayDBVersionedStore(new File("./plasma"))
     avlStorage = new VersionedSwayAVLStorage(swayStore, PlasmaParameters.default)
     plasmaMap = new PlasmaMap[ErgoId, Values.ErgoTree](avlStorage, AvlTreeFlags.AllOperationsAllowed)
  }

  test("Add values"){
    val result = plasmaMap.insert(mockData:_*)
    println(s"Proof: ${result.proof}")
  }

  test("Place in box"){

  }
}

object PlasmaMapSuite {
  val mockData = Seq(
    ergoId("3787a386d1cb7aecee7e482ace984e96488547c0b7c25e6a66e040246b569bcf")
      -> ergoTree("9ffAuiHXqgTCNpPdfJxQ4eQzkG5CPvG5bwEPeQZHLR3tLwvp2Zc"),
    ergoId("c332247d95ac3ade644bcd47de58f553c494843ca470baa546290b63aec0e16c")
      -> ergoTree("9exChj86cfLEV1A3fuGYo4ciBVgHaDHU1UnyHrdBvvtVYFUgUxn"),
    ergoId("3787a386d1cb7aecee7e482ace984e96488547c0b7c25e6a66e040246b569bcf")
      -> ergoTree("9eXbHgHuSRKwMrK5BC8yiv8BmWVpXv9tTPovX2PUtswpmLdTx2o"),
  )

  def ergoId(str: String):    ErgoId = ErgoId.create(str)
  def ergoTree(str: String):  Values.ErgoTree = Address.create(str).getErgoAddress.script
}
