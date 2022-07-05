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

In the Posted Share State design, we'll add one extra byte to the share score. The `paid` byte will
simply indicate whether a miner's shares have been paid out or not. A value of 0 indicates an unpaid
share score, while a value of 1 indicates that the miner has already been paid.

For efficient usage of data, we used the same proof to both lookup the needed miners, as well
as update their values during execution of the contract.

The first draft of the share state contract may be found below:

```scala
{
  val shareState = SELF.R4[AvlTree].get
  val maxScore   = SELF.R5[Int].get


  val rewardBox  = INPUTS(1)

  val initialValue  = rewardBox.R4[Long].get

  val stateUpdates  = getVar[Coll[(Coll[Byte], Coll[Byte])]](0).get
  val proof         = getVar[Coll[Byte]](1).get

  val keys          = stateUpdates.map{ (kv: (Coll[Byte], Coll[Byte])) => kv._1}

  val paymentBoxes: Coll[Box] = OUTPUTS.slice(2, keys.size + 2)
  val totalPaid    = paymentBoxes.fold(0L, { (z: Long, box: Box) => z + box.value })

  // val validReward = SELF.tokens(0)._1 == rewardBox.tokens(0)._1

  val oldScores   = shareState.getMany(keys, proof)

  val validStateTransition = keys.forall{
    (key: Coll[Byte]) =>
      val index =   keys.indexOf(key, 0)
      val output = OUTPUTS(index + 2)
      val oldScoreState: Coll[Byte]  = oldScores(index).get
      val updated: Coll[Byte]   = stateUpdates(index)._2

      val oldScoreBytes = updated.slice(0, 8)

      val shareScore = byteArrayToLong( oldScoreBytes )
      val paymentIsValid = output.value == (shareScore * initialValue) / maxScore.toLong
      val wasNotPaid     = oldScoreState.slice(8, 9)(0) == 0.toByte
      val isNowPaid      = updated.slice(8, 9)(0) == 1.toByte
      val scoresAreEqual = shareScore == byteArrayToLong(updated.slice(0, 8))
      val boxIsCorrect   = blake2b256(output.propositionBytes) == key
      allOf(Coll(
        paymentIsValid,
        wasNotPaid,
        isNowPaid,
        scoresAreEqual,
        boxIsCorrect
      ))

  }
  val nextShareState  = shareState.update(stateUpdates, proof).get
  val nextRewardValid = OUTPUTS(1).value == rewardBox.value - totalPaid
  val nextShareStateValid = OUTPUTS(0).R4[AvlTree].get.digest == nextShareState.digest

  val validSpend = allOf(
    Coll(
      nextRewardValid,
      nextShareStateValid,
      validStateTransition,
    )
  )
  sigmaProp(validSpend)
}
```

### Tests

We broadcasted two different transaction onto the mainnet Ergo blockchain to test out different implementations
of the Posted Share State. In each transaction, we initialized a Share State to contain exactly
2239 miner addresses that have previously mined to Getblok. Each miner's score was randomly generated,
(with some additional score being added to miners who were to be paid out, in order to ensure
box values were greater than 0.001 ERG).

#### Test 1
This earlier implementation did not use context variables, and lacked efficient proof generation.
Instead, the updated key-values were posted in register 4 of `INPUTS(2)`, while the proof was
distributed between `INPUTS(3)` to `INPUTS(7)` and concatenated together within the contract.

The test transaction was able to pay out 90 miners that existed within the Share State.
Transaction cost for this test amounted to 204876, while proof size was around ~18000 bytes.

The transaction may be viewed [here](https://explorer.ergoplatform.com/en/transactions/b16633ecd76e4dc2c104e95656839f475091bb5fb39ff90bb99ff782c98fbeab)

#### Test 2
This test used the implementation shown in the above contract draft. During this test, we used
context variables to hold the key-values and proof bytes, which allowed for a more efficient usage
of blockchain space, as well as adding simplicity (due to not having to concatenate register values).

This test also used more efficient proof generation to pay out miners. Specifically, we selected miners
by first sorting each miner's hashed proposition bytes using their value taken as a BigInteger. We then
used this sorted collection to pay out miner's whose hashed propBytes existed consecutively within the
sorted list.

```scala
mockData.sortBy(m => BigInt(m._1.toHashedStateMiner.bytes)).take(NUM_MINERS)
```

This test transaction allowed us to pay out 300 miners from the Share State, with a transaction cost
of 208094, a 1% increase in transaction cost for a nearly 300% increase in the number of miners paid out.
Moreover, proof size for this transaction was around ~15000 bytes, meaning that the proof for the previous
transaction was nearly 20% larger than the proof for this transaction.

The transaction may be viewed [here](https://explorer.ergoplatform.com/en/transactions/4dc2502e9b0ce5de8c51a34cb4149b0bd2a243b23faef8ca3ddc950d7d35f61f)

----

Transaction cost for both transactions was significantly lower than the cost of the original
SmartPool transactions, which tended to be around 300000 while only paying 10 miners per
transaction (Part of this is due to some additional complexities needed to keep track of balances
in the original contracts).
## Design 2: Persistent Share State