/*
 * Copyright 2020 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.teku.protoarray;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.addExact;
import static java.lang.Math.subtractExact;
import static java.lang.Math.toIntExact;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.datastructures.blocks.BeaconBlock;
import tech.pegasys.teku.datastructures.forkchoice.MutableForkChoiceState;
import tech.pegasys.teku.datastructures.forkchoice.MutableStore;
import tech.pegasys.teku.datastructures.forkchoice.VoteTracker;
import tech.pegasys.teku.datastructures.operations.IndexedAttestation;
import tech.pegasys.teku.datastructures.state.BeaconState;
import tech.pegasys.teku.datastructures.state.Checkpoint;
import tech.pegasys.teku.util.config.Constants;

public class ProtoArrayForkChoiceStrategy implements MutableForkChoiceState {
  private static final Logger LOG = LogManager.getLogger();

  private final ReadWriteLock protoArrayLock = new ReentrantReadWriteLock();
  private final ReadWriteLock votesLock = new ReentrantReadWriteLock();
  private final ReadWriteLock balancesLock = new ReentrantReadWriteLock();
  private final ProtoArray protoArray;

  private Checkpoint justifiedCheckpoint;
  private List<UnsignedLong> balances;

  private ProtoArrayForkChoiceStrategy(
      ProtoArray protoArray, List<UnsignedLong> balances, final Checkpoint justifiedCheckpoint) {
    this.protoArray = protoArray;
    this.balances = balances;
    this.justifiedCheckpoint = justifiedCheckpoint;
  }

  // Public
  public static ProtoArrayForkChoiceStrategy create(
      final Checkpoint finalizedCheckpoint, final Checkpoint justifiedCheckpoint) {
    return create(
        finalizedCheckpoint, justifiedCheckpoint, Constants.PROTOARRAY_FORKCHOICE_PRUNE_THRESHOLD);
  }

  private static ProtoArrayForkChoiceStrategy create(
      final Checkpoint finalizedCheckpoint,
      final Checkpoint justifiedCheckpoint,
      final int pruningThreshold) {
    ProtoArray protoArray =
        new ProtoArray(
            pruningThreshold,
            justifiedCheckpoint.getEpoch(),
            finalizedCheckpoint.getEpoch(),
            new ArrayList<>(),
            new HashMap<>());

    return new ProtoArrayForkChoiceStrategy(protoArray, new ArrayList<>(), justifiedCheckpoint);
  }

  @Override
  public void updateHead(final MutableStore store) {
    Checkpoint justifiedCheckpoint = store.getJustifiedCheckpoint();
    updateForkChoiceWeights(
        store,
        justifiedCheckpoint,
        store.getFinalizedCheckpoint().getEpoch(),
        store.getCheckpointState(justifiedCheckpoint).getBalances().asList());
  }

  @Override
  public Bytes32 getHead() {
    return protoArray.findHead(justifiedCheckpoint.getRoot());
  }

  @Override
  public void onAttestation(final MutableStore store, final IndexedAttestation attestation) {
    votesLock.writeLock().lock();
    try {
      attestation.getAttesting_indices().stream()
          .parallel()
          .forEach(
              validatorIndex -> {
                processAttestation(
                    store,
                    validatorIndex,
                    attestation.getData().getBeacon_block_root(),
                    attestation.getData().getTarget().getEpoch());
              });
    } finally {
      votesLock.writeLock().unlock();
    }
  }

  @Override
  public void onBlock(final BeaconBlock block, final BeaconState state) {
    Bytes32 blockRoot = block.hash_tree_root();
    processBlock(
        block.getSlot(),
        blockRoot,
        block.getParent_root(),
        block.getState_root(),
        state.getCurrent_justified_checkpoint().getEpoch(),
        state.getFinalized_checkpoint().getEpoch());
  }

  @Override
  public void updateFinalizedBlock(Bytes32 finalizedRoot) {
    protoArrayLock.writeLock().lock();
    try {
      protoArray.maybePrune(finalizedRoot);
    } finally {
      protoArrayLock.writeLock().unlock();
    }
  }

  @VisibleForTesting
  void processAttestation(
      MutableStore store,
      UnsignedLong validatorIndex,
      Bytes32 blockRoot,
      UnsignedLong targetEpoch) {
    VoteTracker vote = store.getVote(validatorIndex);

    if (targetEpoch.compareTo(vote.getNextEpoch()) > 0 || vote.equals(VoteTracker.Default())) {
      vote.setNextRoot(blockRoot);
      vote.setNextEpoch(targetEpoch);
    }
  }

  @VisibleForTesting
  void processBlock(
      UnsignedLong blockSlot,
      Bytes32 blockRoot,
      Bytes32 parentRoot,
      Bytes32 stateRoot,
      UnsignedLong justifiedEpoch,
      UnsignedLong finalizedEpoch) {
    protoArrayLock.writeLock().lock();
    try {
      protoArray.onBlock(
          blockSlot, blockRoot, parentRoot, stateRoot, justifiedEpoch, finalizedEpoch);
    } finally {
      protoArrayLock.writeLock().unlock();
    }
  }

  @VisibleForTesting
  void updateForkChoiceWeights(
      final MutableStore store,
      final Checkpoint justifiedCheckpoint,
      final UnsignedLong finalizedEpoch,
      final List<UnsignedLong> justifiedStateBalances) {
    protoArrayLock.writeLock().lock();
    votesLock.writeLock().lock();
    balancesLock.writeLock().lock();
    try {
      List<UnsignedLong> oldBalances = balances;
      List<UnsignedLong> newBalances = justifiedStateBalances;

      List<Long> deltas = computeDeltas(store, protoArray.getIndices(), oldBalances, newBalances);

      this.justifiedCheckpoint = justifiedCheckpoint;
      protoArray.applyScoreChanges(deltas, justifiedCheckpoint.getEpoch(), finalizedEpoch);
      balances = new ArrayList<>(newBalances);
    } finally {
      protoArrayLock.writeLock().unlock();
      votesLock.writeLock().unlock();
      balancesLock.writeLock().unlock();
    }
  }

  public void setPruneThreshold(int pruneThreshold) {
    protoArrayLock.writeLock().lock();
    try {
      protoArray.setPruneThreshold(pruneThreshold);
    } finally {
      protoArrayLock.writeLock().unlock();
    }
  }

  public int size() {
    protoArrayLock.readLock().lock();
    try {
      return protoArray.getNodes().size();
    } finally {
      protoArrayLock.readLock().unlock();
    }
  }

  @Override
  public boolean containsBlock(Bytes32 blockRoot) {
    protoArrayLock.readLock().lock();
    try {
      return protoArray.getIndices().containsKey(blockRoot);
    } finally {
      protoArrayLock.readLock().unlock();
    }
  }

  @Override
  public Optional<UnsignedLong> getBlockSlot(Bytes32 blockRoot) {
    protoArrayLock.readLock().lock();
    try {
      return getProtoNode(blockRoot).map(ProtoNode::getBlockSlot);
    } finally {
      protoArrayLock.readLock().unlock();
    }
  }

  @Override
  public Optional<Bytes32> getBlockParent(Bytes32 blockRoot) {
    protoArrayLock.readLock().lock();
    try {
      return getProtoNode(blockRoot).map(ProtoNode::getParentRoot);
    } finally {
      protoArrayLock.readLock().unlock();
    }
  }

  private Optional<ProtoNode> getProtoNode(Bytes32 blockRoot) {
    return Optional.ofNullable(protoArray.getIndices().get(blockRoot))
        .flatMap(
            blockIndex -> {
              if (blockIndex < protoArray.getNodes().size()) {
                return Optional.of(protoArray.getNodes().get(blockIndex));
              }
              return Optional.empty();
            });
  }

  /**
   * Returns a list of `deltas`, where there is one delta for each of the indices in
   * `0..indices.size()`.
   *
   * <p>The deltas are formed by a change between `oldBalances` and `newBalances`, and/or a change
   * of vote in `votes`.
   *
   * <p>## Errors
   *
   * <ul>
   *   <li>If a value in `indices` is greater to or equal to `indices.size()`.
   *   <li>If some `Bytes32` in `votes` is not a key in `indices` (except for `Bytes32.ZERO`, this
   *       is always valid).
   * </ul>
   *
   * @param indices
   * @param store
   * @param oldBalances
   * @param newBalances
   * @return
   */
  static List<Long> computeDeltas(
      MutableStore store,
      Map<Bytes32, Integer> indices,
      List<UnsignedLong> oldBalances,
      List<UnsignedLong> newBalances) {
    List<Long> deltas = new ArrayList<>(Collections.nCopies(indices.size(), 0L));

    for (UnsignedLong validatorIndex : store.getVotedValidatorIndices()) {
      VoteTracker vote = store.getVote(validatorIndex);

      // There is no need to create a score change if the validator has never voted
      // or both their votes are for the zero hash (alias to the genesis block).
      if (vote.getCurrentRoot().equals(Bytes32.ZERO) && vote.getNextRoot().equals(Bytes32.ZERO)) {
        LOG.warn("ProtoArrayForkChoiceStrategy: Unexpected zero hashes in voted validator votes");
        continue;
      }

      int validatorIndexInt = toIntExact(validatorIndex.longValue());
      // If the validator was not included in the oldBalances (i.e. it did not exist yet)
      // then say its balance was zero.
      UnsignedLong oldBalance =
          oldBalances.size() > validatorIndexInt
              ? oldBalances.get(validatorIndexInt)
              : UnsignedLong.ZERO;

      // If the validator vote is not known in the newBalances, then use a balance of zero.
      //
      // It is possible that there is a vote for an unknown validator if we change our
      // justified state to a new state with a higher epoch that is on a different fork
      // because that may have on-boarded less validators than the prior fork.
      UnsignedLong newBalance =
          newBalances.size() > validatorIndexInt
              ? newBalances.get(validatorIndexInt)
              : UnsignedLong.ZERO;

      if (!vote.getCurrentRoot().equals(vote.getNextRoot()) || !oldBalance.equals(newBalance)) {
        // We ignore the vote if it is not known in `indices`. We assume that it is outside
        // of our tree (i.e. pre-finalization) and therefore not interesting.
        Integer currentDeltaIndex = indices.get(vote.getCurrentRoot());
        if (currentDeltaIndex != null) {
          checkState(
              currentDeltaIndex < deltas.size(), "ProtoArrayForkChoice: Invalid node delta index");
          long delta = subtractExact(deltas.get(currentDeltaIndex), oldBalance.longValue());
          deltas.set(currentDeltaIndex, delta);
        }

        // We ignore the vote if it is not known in `indices`. We assume that it is outside
        // of our tree (i.e. pre-finalization) and therefore not interesting.
        Integer nextDeltaIndex = indices.get(vote.getNextRoot());
        if (nextDeltaIndex != null) {
          checkState(
              nextDeltaIndex < deltas.size(), "ProtoArrayForkChoice: Invalid node delta index");
          long delta = addExact(deltas.get(nextDeltaIndex), newBalance.longValue());
          deltas.set(nextDeltaIndex, delta);
        }

        vote.setCurrentRoot(vote.getNextRoot());
      }
    }
    return deltas;
  }
}
