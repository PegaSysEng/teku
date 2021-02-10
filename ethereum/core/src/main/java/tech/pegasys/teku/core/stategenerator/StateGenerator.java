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

package tech.pegasys.teku.core.stategenerator;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.core.lookup.BlockProvider;
import tech.pegasys.teku.datastructures.blocks.StateAndBlockSummary;
import tech.pegasys.teku.datastructures.hashtree.HashTree;
import tech.pegasys.teku.datastructures.state.BeaconState;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.spec.SpecProvider;

public class StateGenerator {
  public static final int DEFAULT_STATE_CACHE_SIZE = 100;
  private static final Logger LOG = LogManager.getLogger();

  private final HashTree blockTree;
  private final AsyncChainStateGenerator chainStateGenerator;
  @SuppressWarnings("unused")
  private final SpecProvider specProvider;

  private StateGenerator(
      final HashTree blockTree,
      final AsyncChainStateGenerator chainStateGenerator,
      final StateCache stateCache,
      final SpecProvider specProvider) {
    checkArgument(
        stateCache.containsKnownState(blockTree.getRootHash()), "Root state must be available");

    this.blockTree = blockTree;
    this.chainStateGenerator = chainStateGenerator;
    this.specProvider = specProvider;
  }

  public static StateGenerator create(
      final HashTree blockTree,
      final StateAndBlockSummary rootBlockAndState,
      final BlockProvider blockProvider,
      final SpecProvider specProvider) {
    return create(blockTree, rootBlockAndState, blockProvider, Collections.emptyMap(), specProvider);
  }

  public static StateGenerator create(
      final HashTree blockTree,
      final StateAndBlockSummary rootBlockAndState,
      final BlockProvider blockProvider,
      final Map<Bytes32, BeaconState> knownStates,
      final SpecProvider specProvider) {
    return create(
        blockTree, rootBlockAndState, blockProvider, knownStates, DEFAULT_STATE_CACHE_SIZE, specProvider);
  }

  public static StateGenerator create(
      final HashTree blockTree,
      final StateAndBlockSummary rootBlockAndState,
      final BlockProvider blockProvider,
      final Map<Bytes32, BeaconState> knownStates,
      final int stateCacheSize,
      final SpecProvider specProvider) {
    checkArgument(
        rootBlockAndState.getRoot().equals(blockTree.getRootHash()),
        "Provided root block must match the root of the provided block tree");

    final Map<Bytes32, BeaconState> availableStates = new HashMap<>(knownStates);
    availableStates.put(rootBlockAndState.getRoot(), rootBlockAndState.getState());
    final StateCache stateCache = new StateCache(stateCacheSize, availableStates);

    final AsyncChainStateGenerator chainStateGenerator =
        AsyncChainStateGenerator.create(blockTree, blockProvider, stateCache::get, specProvider);
    return new StateGenerator(blockTree, chainStateGenerator, stateCache, specProvider);
  }

  public SafeFuture<StateAndBlockSummary> regenerateStateForBlock(final Bytes32 blockRoot) {
    final int blockCount = blockTree.size() - 1;
    LOG.debug("Regenerate state for block {} by replaying {} blocks", blockRoot, blockCount);
    final long startTime = System.currentTimeMillis();

    return chainStateGenerator
        .generateTargetState(blockRoot)
        .thenPeek(
            result ->
                LOG.info(
                    "Completed regeneration of block {} at slot {} by replaying {} blocks. Took {}ms",
                    blockRoot,
                    result.getSlot(),
                    blockCount,
                    System.currentTimeMillis() - startTime));
  }
}
