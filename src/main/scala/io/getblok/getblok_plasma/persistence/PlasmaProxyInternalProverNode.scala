package io.getblok.getblok_plasma
package persistence

import org.slf4j.{Logger, LoggerFactory}
import scorex.crypto.authds.avltree.batch.{InternalProverNode, ProverNodes, VersionedSwayAVLStorage}
import scorex.crypto.authds.{ADKey, Balance}
import scorex.crypto.hash.{Blake2b256, Digest32}
// Internal prover node from LDB implementation, with set type params and usage of swaydb instead
class PlasmaProxyInternalProverNode(protected var pk: ADKey,
                                    val lkey: ADKey,
                                    val rkey: ADKey,
                                    protected var pb: Balance = Balance @@ 0.toByte)
                                   (implicit val plasmaMap: SwayDBVersionedStore, plasmaParams: PlasmaParameters)
  extends InternalProverNode(k = pk, l = null, r = null, b = pb)(Blake2b256) {
  override def left: ProverNodes[Digest32] = {

    if (l == null){
      l = VersionedSwayAVLStorage.fetch(lkey)
    }
    l
  }

  override def right: ProverNodes[Digest32] = {
    if (r == null) {
      r = VersionedSwayAVLStorage.fetch(rkey)
    }
    r
  }
}
