package io.getblok.getblok_plasma.sway

import boopickle.Default.generatePickler
import scorex.crypto.authds.{ADDigest, ADKey, ADValue}
import scorex.crypto.hash.Digest32
import supertagged.@@
import swaydb.serializers.{BooPickle, Serializer}

object Types {

  case class VersionedDigest(digest: Array[Byte], versionNum: Long)
  case class PlasmaKey(key: Array[Byte])
  case class PlasmaVal(value: Array[Byte])

}
