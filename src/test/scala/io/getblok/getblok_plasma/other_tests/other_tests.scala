package io.getblok.getblok_plasma

import com.google.common.primitives.Longs
import org.ergoplatform.appkit._
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.slf4j.{Logger, LoggerFactory}
import scorex.crypto.authds.ADKey
import sigmastate.eval.Colls
import special.collection.Coll
import special.sigma.AvlTree
import supertagged.@@

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

package object other_tests {
  val ergoClient: ErgoClient = RestApiErgoClient.create("http://188.34.207.91:9053/", NetworkType.MAINNET, "", "")
  val creatorAddress: Address = Address.create("4MQyML64GnzMxZgm")
  val dummyTxId = "ce552663312afc2379a91f803c93e2b10b424f176fbc930055c10def2fd88a5d"
  val dummyToken = "f5cc03963b64d3542b8cea49d5436666a97f6a2d098b7d3b2220e824b5a91819"
  val dummyTokenId: ErgoId = ErgoId.create(dummyToken)
  def logger: Logger = LoggerFactory.getLogger("AVLTests")

  def buildUserBox(value: Long): InputBox = {
    ergoClient.execute{
      ctx =>
        val inputBox = ctx.newTxBuilder().outBoxBuilder()
          .value(value)
          .contract(new ErgoTreeContract(creatorAddress.getErgoAddress.script, NetworkType.MAINNET))
          .build()
          .convertToInputWith("ce552663312afc2379a91f803c93e2b10b424f176fbc930055c10def2fd88a5d", 0)
        sigma.util.Extensions
        return inputBox
    }
  }

  def buildAVLBox(value: Long, key: Array[Byte], tree: ErgoValue[AvlTree], proof: ErgoValue[Coll[Byte]]): InputBox = {
    ergoClient.execute{
      ctx =>
        val inputBox = ctx.newTxBuilder().outBoxBuilder()
          .value(value)
          .contract(keyExistContract(key)(ctx))
          .registers(tree, proof)
          .build()
          .convertToInputWith("ce552663312afc2379a91f803c93e2b10b424f176fbc930055c10def2fd88a5d", 0)
        return inputBox
    }
  }

  def buildGetManyAVLBoxes(value: Long, keys: Seq[Array[Byte]], tree: ErgoValue[AvlTree], proof: ErgoValue[Coll[Byte]]): Seq[InputBox] = {
    ergoClient.execute{
      ctx =>
        val avlBox = ctx.newTxBuilder().outBoxBuilder()
          .value(value / 2)
          .contract(getManyContract(ctx))
          .registers(tree, ErgoValue.of(Colls.fromArray(keys.toArray.map(k => Colls.fromArray(k))), ErgoType.collType(ErgoType.byteType())))
          .build()
          .convertToInputWith("ce552663312afc2379a91f803c93e2b10b424f176fbc930055c10def2fd88a5d", 0)

        val proofBox = ctx.newTxBuilder().outBoxBuilder()
          .value(value / 2)
          .contract(sigmaTrueContract(ctx))
          .registers(proof)
          .build()
          .convertToInputWith("ce552663312afc2379a91f803c93e2b10b424f176fbc930055c10def2fd88a5d", 0)
        return Seq(avlBox, proofBox)
    }
  }

  val keyExistScript =
    """
      | {
      | val tree  = SELF.R4[AvlTree].get
      | val proof = SELF.R5[Coll[Byte]].get
      | val key   = const_key
      | val keyExists = tree.get(key, proof).isDefined
      |
      | sigmaProp(keyExists)
      | }
      |""".stripMargin


  val getManyScript =
    """
      | {
      | val tree  = SELF.R4[AvlTree].get
      | val keys  = SELF.R5[Coll[Coll[Byte]]].get
      | val proof = INPUTS(1).R4[Coll[Byte]].get
      | val keysExist = tree.getMany(keys, proof).forall{
      |   (o: Option[Coll[Byte]]) =>
      |     o.isDefined
      | }
      |
      | sigmaProp(keysExist)
      | }
      |""".stripMargin

  def keyExistContract(key: Array[Byte])(implicit ctx: BlockchainContext) = {
    ctx.compileContract(ConstantsBuilder.create().item("const_key", Colls.fromArray(key)).build(), keyExistScript)
  }

  def getManyContract(implicit ctx: BlockchainContext) = {
    ctx.compileContract(ConstantsBuilder.create().build(), getManyScript)
  }

  def sigmaTrueContract(ctx: BlockchainContext) = {
    ctx.compileContract(ConstantsBuilder.empty(), """ { sigmaProp(true) } """)
  }

  def getInputBoxes: Array[InputBox] = Array(buildUserBox(Parameters.OneErg * 122))

  def dummyProver: ErgoProver = {
    ergoClient.execute{
      ctx =>
        val prover = ctx.newProverBuilder()
          .withDLogSecret(BigInt.apply(0).bigInteger)
          .build()

        return prover
    }
  }

  def randomLongKey: Array[Byte] @@ ADKey.Tag = {
    ADKey @@ Longs.toByteArray(Random.nextInt(100000) + Parameters.MinFee)
  }



}