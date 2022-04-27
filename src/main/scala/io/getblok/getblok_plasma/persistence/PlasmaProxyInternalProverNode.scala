package io.getblok.getblok_plasma
package persistence


import io.getblok.getblok_plasma.sway.SwayDBVersionedStore
import org.slf4j.{Logger, LoggerFactory}
import scorex.crypto.authds.avltree.batch.{InternalProverNode, ProverNodes, VersionedPlasmaStorage}
import scorex.crypto.authds.{ADKey, Balance}
import scorex.crypto.hash.{Blake2b256, Digest32}

class PlasmaProxyInternalProverNode(protected var pk: ADKey,
                                    val lkey: ADKey,
                                    val rkey: ADKey,
                                    protected var pb: Balance = Balance @@ 0.toByte)
                                   (implicit val plasmaMap: SwayDBVersionedStore, plasmaParams: PlasmaParameters)
  extends InternalProverNode(k = pk, l = null, r = null, b = pb)(Blake2b256) {
  private val logger: Logger = LoggerFactory.getLogger("PlasmaInternalNode")
  override def left: ProverNodes[Digest32] = {

    if (l == null){
      logger.info("Going left")
      l = VersionedPlasmaStorage.fetch(lkey)
    }
    l
  }

  override def right: ProverNodes[Digest32] = {
    if (r == null) {
      logger.info("Going right")
      r = VersionedPlasmaStorage.fetch(rkey)
    }
    r
  }
}
