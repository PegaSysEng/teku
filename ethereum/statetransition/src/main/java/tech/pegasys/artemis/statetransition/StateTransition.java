/*
 * Copyright 2019 ConsenSys AG.
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

package tech.pegasys.artemis.statetransition;

import com.google.common.primitives.UnsignedLong;
import net.consensys.cava.bytes.Bytes32;
import org.apache.logging.log4j.Level;
import sun.jvm.hotspot.opto.Block;
import tech.pegasys.artemis.datastructures.Constants;
import tech.pegasys.artemis.datastructures.blocks.BeaconBlock;
import tech.pegasys.artemis.datastructures.state.BeaconState;
import tech.pegasys.artemis.datastructures.state.BeaconStateWithCache;
import tech.pegasys.artemis.datastructures.util.BeaconStateUtil;
import tech.pegasys.artemis.statetransition.util.BlockProcessingException;
import tech.pegasys.artemis.statetransition.util.BlockProcessorUtil;
import tech.pegasys.artemis.statetransition.util.EpochProcessingException;
import tech.pegasys.artemis.statetransition.util.EpochProcessorUtil;
import tech.pegasys.artemis.statetransition.util.PreProcessingUtil;
import tech.pegasys.artemis.statetransition.util.SlotProcessingException;
import tech.pegasys.artemis.statetransition.util.SlotProcessorUtil;
import tech.pegasys.artemis.util.alogger.ALogger;
import tech.pegasys.artemis.util.hashtree.HashTreeUtil;

import static tech.pegasys.artemis.datastructures.Constants.*;

public class StateTransition {

  private static final ALogger LOG = new ALogger(StateTransition.class.getName());

  private boolean printEnabled = false;

  public StateTransition() {}

  public StateTransition(boolean printEnabled) {
    this.printEnabled = printEnabled;
  }

  public void initiate(BeaconStateWithCache state, BeaconBlock block, Bytes32 previous_block_root)
      throws StateTransitionException {
    state.incrementSlot();
    // pre-process and cache selected state transition calculations
    preProcessor(state);
    // per-slot processing
    slotProcessor(state, previous_block_root);
    // per-block processing
    if (block != null) {
      blockProcessor(state, block);
    }
    // per-epoch processing
    if (state
        .getSlot()
        .plus(UnsignedLong.ONE)
        .mod(UnsignedLong.valueOf(SLOTS_PER_EPOCH))
        .equals(UnsignedLong.ZERO)) {
      epochProcessor(state, block);
    }
    // reset all cached state variables
    state.invalidateCache();
  }

  protected void preProcessor(BeaconStateWithCache state) {
    // calculate currentBeaconProposerIndex
    PreProcessingUtil.cacheCurrentBeaconProposerIndex(state);
  }

  protected void advance_slot(BeaconStateWithCache state) {
    state.getLatest_state_roots().set(state.getSlot().mod(UnsignedLong.valueOf(SLOTS_PER_HISTORICAL_ROOT)).intValue(), HashTreeUtil.hash_tree_root(state.toBytes()));
    state.incrementSlot();
    if (state.getLatest_block_header().getState_root() == ZERO_HASH) {
      state.getLatest_block_header().setState_root(BeaconStateUtil.get_state_root(state, state.getSlot().minus(UnsignedLong.ONE)));
    }
    state.getLatest_block_roots().set((state.getSlot().minus(UnsignedLong.ONE).mod(UnsignedLong.valueOf(SLOTS_PER_HISTORICAL_ROOT)).intValue()), HashTreeUtil.hash_tree_root(state.getLatest_block_header().toBytes()));)
  }

  protected void slotProcessor(BeaconStateWithCache state, Bytes32 previous_block_root) {
    try {
      // Slots the proposer has skipped (i.e. layers of RANDAO expected)
      // should be in Validator.randao_skips
      SlotProcessorUtil.updateBlockRoots(state, previous_block_root);
    } catch (SlotProcessingException e) {
      LOG.log(Level.WARN, "  Slot processing error: " + e, printEnabled);
    }
  }

  private void blockProcessor(BeaconStateWithCache state, BeaconBlock block) {
    try {

      BlockProcessorUtil.process_block_header(state, block);
      BlockProcessorUtil.process_randao(state, block);
      BlockProcessorUtil.process_eth1_data(state, block);
      BlockProcessorUtil.proposer_slashing(state, block);
      BlockProcessorUtil.attester_slashing(state, block);
      BlockProcessorUtil.processAttestations(state, block);
      BlockProcessorUtil.processDeposits(state, block);
      BlockProcessorUtil.processVoluntaryExits(state, block);
      BlockProcessorUtil.processTransfers(state, block);
      BlockProcessorUtil.verify_block_state_root(state, block);

      } catch (BlockProcessingException e) {
        LOG.log(Level.WARN, "  Block processing error: " + e, printEnabled);
      }
  }

  private void epochProcessor(BeaconStateWithCache state, BeaconBlock block) {
    try {
      String ANSI_YELLOW_BOLD = "\033[1;33m";
      String ANSI_RESET = "\033[0m";
      if (printEnabled) System.out.println();
      LOG.log(
          Level.INFO,
          ANSI_YELLOW_BOLD + "********  Processing new epoch: " + " ********* " + ANSI_RESET,
          printEnabled);

      LOG.log(
          Level.INFO,
          "Epoch:                                  "
              + BeaconStateUtil.get_current_epoch(state)
              + " |  "
              + BeaconStateUtil.get_current_epoch(state).longValue() % Constants.GENESIS_EPOCH,
          printEnabled);

      EpochProcessorUtil.updateEth1Data(state);
      LOG.log(Level.DEBUG, "updateEth1Data()", printEnabled);
      EpochProcessorUtil.updateJustification(state, block);
      LOG.log(Level.DEBUG, "updateJustification()", printEnabled);
      EpochProcessorUtil.updateCrosslinks(state);
      LOG.log(Level.DEBUG, "updateCrosslinks()", printEnabled);

      UnsignedLong previous_total_balance = BeaconStateUtil.previous_total_balance(state);
      LOG.log(Level.DEBUG, "justificationAndFinalization()", printEnabled);
      EpochProcessorUtil.justificationAndFinalization(state, previous_total_balance);
      LOG.log(Level.DEBUG, "attestionInclusion()", printEnabled);
      EpochProcessorUtil.attestionInclusion(state, previous_total_balance);
      LOG.log(Level.DEBUG, "crosslinkRewards()", printEnabled);
      EpochProcessorUtil.crosslinkRewards(state, previous_total_balance);

      LOG.log(Level.DEBUG, "process_ejections()", printEnabled);
      EpochProcessorUtil.process_ejections(state);

      LOG.log(Level.DEBUG, "previousStateUpdates()", printEnabled);
      EpochProcessorUtil.previousStateUpdates(state);
      if (EpochProcessorUtil.shouldUpdateValidatorRegistry(state)) {
        LOG.log(Level.DEBUG, "update_validator_registry()", printEnabled);
        EpochProcessorUtil.update_validator_registry(state);
        LOG.log(Level.DEBUG, "currentStateUpdatesAlt1()", printEnabled);
        EpochProcessorUtil.currentStateUpdatesAlt1(state);
      } else {
        LOG.log(Level.DEBUG, "currentStateUpdatesAlt2()", printEnabled);
        EpochProcessorUtil.currentStateUpdatesAlt2(state);
      }
      LOG.log(Level.DEBUG, "process_penalties_and_exits()", printEnabled);
      EpochProcessorUtil.process_penalties_and_exits(state);
      LOG.log(Level.DEBUG, "finalUpdates()", printEnabled);
      EpochProcessorUtil.finalUpdates(state);
    } catch (EpochProcessingException e) {
      LOG.log(Level.WARN, "  Epoch processing error: " + e, printEnabled);
    }
  }
}
