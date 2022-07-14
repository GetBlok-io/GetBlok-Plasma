package io.getblok.getblok_plasma.collections

import org.bouncycastle.util.encoders.Hex

/**
 * A Manifest represents the total data needed to manually reconstruct the underlying AVL Tree.
 * Each manifest is associated with its digest bytes, manifest bytes, and subtree bytes.
 * This set of byte arrays may then be used to load a tree without the need of a dedicated local database.
 * @param digest Bytes associated with the PlasmaMap's digest
 * @param bytes Main manifest bytes needed for tree reconstruction
 * @param subTrees Set of subtree bytes needed for tree reconstruction
 */
case class Manifest(digest: Array[Byte], bytes: Array[Byte], subTrees: Seq[Array[Byte]]){

  /**
   * Convert the reconstruction bytes of the tree into hexadecimal string values
   * @return A pair holding the hexadecimal representations of the manifest and it's associated subtrees
   */
  def toHexStrings: (String, Seq[String]) = {
    manifestString -> subtreeStrings
  }

  /**
   * @return Digest bytes in hexadecimal string form
   */
  def digestString:   String        = Hex.toHexString(digest)

  /**
   * @return Manifest bytes in hexadecimal string form
   */
  def manifestString: String        = Hex.toHexString(bytes)

  /**
   * @return A sequence of subtree bytes in hexadecimal string form
   */
  def subtreeStrings: Seq[String]   = subTrees.map(Hex.toHexString)

  override def toString: String = {
    val strings = toHexStrings
    s"${strings._1},${strings._2.slice(0, strings._2.size).map(_+",") + strings._2.lastOption.getOrElse("")}"
  }
}

object Manifest {
  /**
   * Creates a manifest object using hexadecimal string representations
   * @param digestString Hexadecimal string representing the tree digest
   * @param manifestString Hexadecimal string representing the tree manifest
   * @param subtreeStrings A sequence of hexadecimal strings representing the subtrees associated with the Manifest
   * @return A new Manifest created from the given string representations
   */
  def fromHexStrings(digestString: String, manifestString: String, subtreeStrings: Seq[String]): Manifest = {
    val digestBytes   = Hex.decode(digestString)
    val manifestBytes = Hex.decode(manifestString)
    val subtreeBytes  = subtreeStrings.map(Hex.decode)
    Manifest(digestBytes, manifestBytes, subtreeBytes)
  }
}
