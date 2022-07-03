package io.getblok.getblok_plasma.collections

case class ProvenResult[V](response: Seq[OpResult[V]], proof: Proof)
