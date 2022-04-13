package io.getblok.getblok_plasma
package persistence

import scorex.crypto.authds.avltree.batch.{InternalProverNode, ProverNodes}
import scorex.crypto.authds.{ADKey, Balance}
import scorex.crypto.hash.{Blake2b256, Digest32}

class ProxyInternalProverNode(protected var pk: ADKey,
                                           val lkey: ADKey,
                                           val rkey: ADKey,
                                           protected var pb: Balance = Balance @@ 0.toByte)
                                          (implicit val plasmaMap: VersionedPlasmaMap, plasmaParams: PlasmaParameters)
  extends InternalProverNode(k = pk, l = null, r = null, b = pb)(Blake2b256) {

  override def left: ProverNodes[Digest32] = {
    if (l == null) l = VersionedPlasmaStorage.fetch(lkey)
    l
  }

  override def right: ProverNodes[Digest32] = {
    if (r == null) r = VersionedPlasmaStorage.fetch(rkey)
    r
  }
}
