# GetBlok Plasma
GetBlok Plasma is a library on top of Ergo Appkit that provides an abstraction layer to simplify
the process of integrating AVL Trees (AKA *Plasma*) into off-chain code. The goal is to give developers an easy way 
to use this Layer-2 scaling solution in contracts, off-chain code, and distributed systems managing the
Plasma itself. GetBlok Plasma uses a versioned storage implementation powered by **SwayDB**, allowing
for distributed systems to keep track of the key-value pairs held in digests stored on-chain.

## Details

Currently, GetBlok Plasma makes a few assumptions to make things easier for new developers working with 
AVL Trees. The following is true for every AVL Tree made in GetBlok Plasma:
- Digests from the AVL Tree are 32 bytes in length (+ 1 for tree height)
- Key and label sizes are the same length, and also have a size of 32 bytes
- Value sizes may be optionally set to any number, but the default value is to have no restriction
- Each AVLTree uses Blake2b256 hashing, as this is the standard hashing function used on the Ergo blockchain

Digests stored in SwayDB are associated with some version number. When putting and removing key-value
pairs, one must provide a digest representing the new state of the tree. During the process of updating
the tree, all key-value pairs are taken from the last existing version of the tree and are placed
into the new tree version. From there, removals and inserts are operated on the new version so that
the digest provided is properly mapped to the key-value pairs it represents.

# Serialization
The key-value pairs associated with an AVL Tree digest are serialized into a SwayDB `MultiMap`. This
is the versioned storage implementation that allows for usage of the `PersistentBatchAVLProver`, essentially
a class that keeps track changes made to the AVL Tree. Every change in the AVL Tree creates a new unique
digest, which is stored as the AVL Tree's *version*. 

We use three different classes representing versions/digests, keys, and values present in the AVL Tree.
- `PlasmaKey` represents a simple array of bytes corresponding to some key in the AVL Tree. As per
the details above, each `PlasmaKey` is 32 bytes in length
- `PlasmaVal` represents an array of bytes corresponding to a value in the AVL Tree. `PlasmaVal` may be of
any size, unless the `valueSizeOpt` is set in `PlasmaParameters`.
- `VersionedDigest` is an array of bytes representing the digest of the AVL Tree that is linked to some
set of `PlasmaKey -> PlasmaVal` pairs. Each `VersionedDigest` is serialized in the following way:
  - The first 33 bytes of the `VersionedDigest` represent the digest taken from the AVL Tree's prover. 32 of these
  bytes represent the actual digest, while the last byte represents the AVL Tree's height.
  - All bytes after the first 33 represent a `Long` corresponding to the version number of the tree.

For storage in SwayDB, GetBlok Plasma creates a new `child` within the `MultiMap`. Each child may be
directly called using the `VersionedDigest` associated with it. Knowledge of the version number is not
needed, as it is assumed that no two `VersionedDigest`s will use the same digest value.

Once a child is created, reading key-value pairs is as simple as providing the `VersionedDigest` and
calling `get(k: PlasmaKey)` on the `MultiMap` returned by the `child`. If you only wish to read key-value
pairs from the last version, then you may call `get(k: PlasmaKey)` or `apply(k: PlasmaKey)` on the
`SwayDBVersionedStore` itself. Traversing the AVL Tree for a specific value may be done using `VersionedPlasmaStorage.fetch(k: ADKey)`
These functions are rather low-level though, and as more abstractions are made it should be unnecessary
to call them directly.