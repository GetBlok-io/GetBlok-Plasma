# Mining Pool Operating At Layer 2

In this document, we'll be going through a few different designs for operating a mining pool on
layer 2. To begin, we should specify that the goal of a mining pool is to keep track of and hold *shares*.
Shares represent potential solutions to the PoW puzzle, and are used to calculate how much work a miner performed
relative to the rest of the pool. Shares can therefore be used to derive a payout for a miner. Each share
in a mining pool has an associated *difficulty*. The difficulty of a share is determined by how close the
hash of the potential PoW solution was to the real target solution provided by the blockchain. 

By multiplying each of a miner's shares by their difficulty and then taking the sum of these values, we may come up with the accumulated
difficulty a miner has submitted to the pool. We'll call this value the *share score*, and it will be our main
focus while looking at ways to create a layer 2 mining pool. Different payment methods may use or calculate 
the share score in slightly different ways (PPLNS takes the share score within a window, while PPS takes the entire share score, etc.),
but for simplicity's sake, we will keep such calculations off chain. Instead, our goal is to create
a scalable contract which will pay miners a proportion of the block reward according to the share score
they were assigned.

The current smart pool implementation used by Getblok involves multiple boxes, which are spent through multi-stage 
chained transactions. While this solution does work, it's still limited by
on-chain storage space, specifically that one box may only hold 4096 bytes. AVL Trees seem to be the natural
solution to this problem, allowing us to store hundreds of miners within a single box. Moreover, usage
of AVL Trees will provide simplicity to the entire process, and will allow the smart pool to be
more efficient (while still ensuring transparency).

To begin, we will define a single AVL Tree that will be used to keep track of miners and their share scores.
We'll call this tree the *share state*, and it will be a simple key-value mapping of the blake2b256
hash of a miner's proposition bytes(32 bytes) to the miner's share score(an 8-byte long value).

```scala
blake2b256(miner.propositionBytes) -> shareScore
```
The share state will be our base for the two following designs, each with their own benefits and drawbacks.

## Design 1: Posted Share State

Posted share states represent individual boxes on the blockchain, each one with a separate
share state AVL tree existing within its registers. Share state boxes are sent to the Ergo blockchain
after a block (or number of blocks) is found. Each share state is created and associated with a
*reward box*, which holds the block reward(s) that help determine payouts for miners present
within the state tree. 