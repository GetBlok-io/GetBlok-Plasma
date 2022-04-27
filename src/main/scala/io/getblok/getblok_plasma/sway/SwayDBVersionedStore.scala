package io.getblok.getblok_plasma.sway
import boopickle.Default.generatePickler
import com.google.common.primitives.Longs
import io.getblok.getblok_plasma.sway.Serializers.{DigestSerializer, KeySerializer, ValSerializer}
import io.getblok.getblok_plasma.sway.Types.{PlasmaKey, PlasmaVal, VersionedDigest}
import org.slf4j.LoggerFactory
import scorex.crypto.authds.{ADKey, ADValue}
import scorex.crypto.hash.Digest32
import supertagged.@@
import swaydb.Bag.tryBag
import swaydb.PureFunctionScala.{OnKeyValue, OnValue}
import swaydb.core.map.counter.PersistentCounterMapCache
import swaydb.data.Functions
import swaydb.{Apply, Glass, MultiMap, PureFunction}
import swaydb.persistent._
import swaydb.serializers.Default.{ByteArraySerializer, IntSerializer}
import swaydb.serializers.Serializer

import java.io.File
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Try}
class SwayDBVersionedStore(dir: File){
  val logger = LoggerFactory.getLogger("SwayDBVersionedStore")


  private var multiMap: Try[MultiMap[VersionedDigest, PlasmaKey, PlasmaVal, Nothing, Try]] = _

  def open() = multiMap = swaydb.persistent.MultiMap[VersionedDigest, PlasmaKey, PlasmaVal, Nothing, Try](dir = dir.toPath)
  def close() = {
    multiMap.map(m => m.close())
    multiMap = null
  }


  def versions: Option[Array[VersionedDigest]] =
    Try(multiMap.get.children.iterator[Try].toArray.map(m => m.map(t => t.mapKey).get).sortBy(t => t.versionNum)).toOption

  def getVersion(digest: Array[Byte]): Option[VersionedDigest] =
    versions.flatMap(o => o.find(v => v.digest sameElements digest))

  def lastVersion: Option[VersionedDigest] = versions.flatMap(o => o.lastOption)
  def firstVersion: Option[VersionedDigest] = versions.flatMap(o => o.headOption)

  def getAtVersion(v: VersionedDigest): Option[MultiMap[VersionedDigest, PlasmaKey, PlasmaVal, Nothing, Try]] = multiMap.get.getChild(v).toOption.flatten

  def insert(digest: Array[Byte], toInsert: Seq[(PlasmaKey, PlasmaVal)]): Try[Unit] = update(digest, Seq.empty, toInsert)
  def remove(digest: Array[Byte], toRemove: Seq[PlasmaKey]): Try[Unit] = update(digest, toRemove, Seq.empty )
  def get(key: PlasmaKey): Option[PlasmaVal] = lastVersion.flatMap(lv => getAtVersion(lv)).flatMap(m => m.get(key).toOption).flatten
  def apply(key: PlasmaKey): PlasmaVal =  lastVersion.flatMap(m => getAtVersion(m)).map(o => o.get(key)).getOrElse({
    throw new NoSuchElementException()
  }).get.get
  /**
   * Updates VersionedStore with new version, along with added and removed values. Source is closed after updating.
   * @param version new Version to add
   * @param added Added values
   * @param removed Removed values
   */
  def update(digest: Array[Byte], removed: Seq[PlasmaKey], added: Seq[(PlasmaKey, PlasmaVal)]): Try[Unit] = {
    Try {

      logger.info(s"Updating digest to: ${toHex(digest)}")
      val lastDigestVers = lastVersion
      val nextVersion = {
        if(lastDigestVers.isDefined)
          VersionedDigest(digest, lastDigestVers.get.versionNum + 1)
        else
          VersionedDigest(digest, 0)
      }
      val nextChild = multiMap.get.child(nextVersion)




      // Load new version with values from last version
      if(lastDigestVers.isDefined) {
        logger.info(s"Last Version: ${toHex(lastDigestVers.get.digest)}")
        val lastVersionPairs = multiMap.get.child(lastDigestVers.get).get.stream
        lastVersionPairs.foreach(p => logger.info(s"Adding keyVal from last version (${toHex(p._1.key)} -> ${toHex(p._2.value)})"))
        nextChild.get.put(lastVersionPairs)
      }

       //Now add and remove updated values
      if (removed.nonEmpty) {
        removed.foreach(a => logger.info(s"Now removing key (${toHex(a.key)}) with length ${a.key.length}"))
        nextChild.get.remove(removed)
      }

      if (added.nonEmpty) {
          added.foreach {
            a =>
              logger.info(s"Now adding keyVal (${toHex(a._1.key)} -> ${toHex(a._2.value)}) with lengths (${a._1.key.length} -> ${a._2.value.length}")
              nextChild.get.put(a)
          }

        }
      ()
    }.recoverWith{
      case e =>
      logger.error("There was a critical error!", e)
        Failure(e)
    }
  }
  def toHex(arr: Array[Byte]) = BigInt(arr).toString(16)
  def rollbackTo(digest: Array[Byte]): Try[Unit] = {
    Try {
      val rollbackVersion = getVersion(digest).get
      logger.info(s"Rolling back to digest ${toHex(rollbackVersion.digest)}")

      val versionsToRemove = versions.get.filter(v => v.versionNum > rollbackVersion.versionNum)
      val rolledBackKVs = getAtVersion(rollbackVersion).get.stream
      rolledBackKVs.foreach(cv => logger.info(s"Found keyVal (${toHex(cv._1.key)} -> ${toHex(cv._2.value)}) with lengths (${cv._1.key.length} -> ${cv._2.value.length}"))

      versionsToRemove.foreach{
        v =>
        logger.info(s"Now removing child version ${toHex(v.digest)} with version number ${v.versionNum} ")
        multiMap.get.removeChild(v)
      }
    }

  }


}
object SwayDBVersionedStore {

}
