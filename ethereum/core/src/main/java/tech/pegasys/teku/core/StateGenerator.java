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

package tech.pegasys.teku.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.datastructures.blocks.SignedBeaconBlock;
import tech.pegasys.teku.datastructures.state.BeaconState;

public class StateGenerator {
  private static final Logger LOG = LogManager.getLogger();

  /**
   * Given a base state and some blocks, processes the given blocks on top of the base state to
   * produce the states belonging to each block.
   *
   * @param baseStateBlockRoot The block root corresponding to the base state.
   * @param baseState The base state to build on top of.
   * @param newBlocks A list of blocks to process on top of the base state.
   * @return A map from blockRoot to state containing the base state and all other states that could
   *     be successfully generated.
   */
  public Map<Bytes32, BeaconState> produceStatesForBlocks(
      final Bytes32 baseStateBlockRoot,
      final BeaconState baseState,
      final Collection<SignedBeaconBlock> newBlocks) {
    final Map<Bytes32, BeaconState> statesByRoot = new HashMap<>();

    // Initialize states with the base state
    statesByRoot.put(baseStateBlockRoot, baseState);

    // Index blocks by parent root
    final Map<Bytes32, List<SignedBeaconBlock>> blocksByParent = new HashMap<>();
    for (SignedBeaconBlock currentBlock : newBlocks) {
      final List<SignedBeaconBlock> blockList =
          blocksByParent.computeIfAbsent(currentBlock.getParent_root(), (key) -> new ArrayList<>());
      blockList.add(currentBlock);
    }

    // Generate states
    final Deque<Bytes32> parentRoots = new ArrayDeque<>();
    parentRoots.push(baseStateBlockRoot);
    while (!parentRoots.isEmpty()) {
      final Bytes32 parentRoot = parentRoots.pop();
      final BeaconState parentState = statesByRoot.get(parentRoot);
      final List<SignedBeaconBlock> blocks =
          blocksByParent.computeIfAbsent(parentRoot, (key) -> Collections.emptyList());
      for (SignedBeaconBlock block : blocks) {
        final Bytes32 blockRoot = block.getMessage().hash_tree_root();
        processBlock(parentState, block)
            .ifPresent(
                state -> {
                  statesByRoot.put(blockRoot, state);
                  parentRoots.push(blockRoot);
                });
      }
    }

    return statesByRoot;
  }

  private Optional<BeaconState> processBlock(
      final BeaconState preState, final SignedBeaconBlock block) {
    StateTransition stateTransition = new StateTransition();
    try {
      final BeaconState postState = stateTransition.initiate(preState, block);
      return Optional.of(postState);
    } catch (StateTransitionException e) {
      LOG.trace(
          "Unable to produce state for block at slot {} ({})",
          block.getSlot(),
          block.getMessage().hash_tree_root());
      return Optional.empty();
    }
  }
}
