package io.getblok.getblok_plasma
package persistence

import io.getblok.getblok_plasma.persistence.VersionedPlasmaMap.{Key, Value}
import scorex.crypto.authds.{ADDigest, ADKey, ADValue}
import supertagged.@@

import scala.collection.immutable.ListMap

class VersionedPlasmaMap(val versionedByteMap: ListMap[ADDigest, ListMap[Key, Value]]) {
  /**
   * Get version of PlasmaMap
   * @param digest Digest to get version by
   * @return SortedMap of keys to values in the current version
   */
  def getVersion(digest: ADDigest): ListMap[Key, Value] = versionedByteMap(digest)

  /**
   * Get most value from most recent version using key
   * @param key ADKey
   * @return ADValue
   */
  def apply(key: Key): Value = versionedByteMap.last._2(key)
}

object VersionedPlasmaMap {
  type Key    = Array[Byte] @@ ADKey.Tag
  type Value  = Array[Byte] @@ ADValue

  /** Default mapping takes array of bytes and tags them */
  def fromByteMappings(byteMappings: ListMap[Array[Byte], ListMap[Array[Byte], Array[Byte]]]): VersionedPlasmaMap = {

    val tagged = ListMap(byteMappings.map(versions => ADDigest @@ versions._1 ->
      ListMap(versions._2.map(maps => ADKey @@ maps._1 -> ADValue @@ maps._2).toArray:_*)).toArray:_*)
    new VersionedPlasmaMap(tagged)
  }

  /** Takes mappings of hex string and builds them into VersionedPlasmaMap */
  def fromHexMapping(hexMappings: ListMap[String, ListMap[String, String]]): VersionedPlasmaMap = {
    val byteMappings = fromByteConversion(hexMappings)
    byteMappings
  }

  /**
   * Takes any convertible set of mappings and creates a VersionedPlasmaMap
   */
  def fromByteConversion[D, K, V](conversionMap: ListMap[D, ListMap[K, V]])
                                 (implicit digestConvertible: ByteConversion[D], keyConvertible: ByteConversion[K],
                                  valConvertible: ByteConversion[V]): VersionedPlasmaMap = {
    val convertedMap = conversionMap.map(conv => digestConvertible.convertToBytes(conv._1)
      -> ListMap(conv._2.map(kv => keyConvertible.convertToBytes(kv._1) -> valConvertible.convertToBytes(kv._2)).toSeq:_*))
    fromByteMappings(convertedMap)
  }

}
