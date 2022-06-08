package io.getblok.getblok_plasma.persistence

object Types {

  case class VersionedDigest(digest: Array[Byte], versionNum: Long)

  case class PlasmaKey(key: Array[Byte])

  case class PlasmaVal(value: Array[Byte])

}
