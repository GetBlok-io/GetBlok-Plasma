package io.getblok.getblok_plasma.sway

import io.getblok.getblok_plasma.sway.Types.{PlasmaKey, PlasmaVal, VersionedDigest}
import scorex.crypto.authds.ADDigest
import scorex.crypto.hash.Digest32
import supertagged.@@
import swaydb.data.slice.Slice
import swaydb.serializers.Serializer

object Serializers {
  implicit object ValSerializer extends Serializer[PlasmaVal] {
    override def write(data: PlasmaVal): Slice[Byte] =
      Slice(data.value)

    override def read(slice: Slice[Byte]): PlasmaVal =
      PlasmaVal(slice.toArray)
  }
  implicit object KeySerializer extends Serializer[PlasmaKey] {
    override def write(data: PlasmaKey): Slice[Byte] =
      Slice(data.key)

    override def read(slice: Slice[Byte]): PlasmaKey =
      PlasmaKey(slice.toArray)
  }

  // Serializes first 33 bytes of digest, after which come additional bytes for the version number
  implicit object DigestSerializer extends Serializer[VersionedDigest] {
    override def write(data: VersionedDigest): Slice[Byte] =
      Slice((data.digest ++ BigInt(data.versionNum).toByteArray))

    override def read(slice: Slice[Byte]): VersionedDigest = {
      val sliceArr = slice.toArray
      val digest = sliceArr.slice(0, 33)
      val bigint = sliceArr.slice(33, sliceArr.length)
      VersionedDigest(digest, BigInt(bigint).toLong)
    }
  }
}
