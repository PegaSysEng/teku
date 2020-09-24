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

package tech.pegasys.teku.services.beaconchain;

import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import tech.pegasys.teku.datastructures.blocks.NodeSlot;
import tech.pegasys.teku.datastructures.state.BeaconState;
import tech.pegasys.teku.datastructures.state.Checkpoint;
import tech.pegasys.teku.datastructures.state.PendingAttestation;
import tech.pegasys.teku.infrastructure.metrics.SettableDoubleGauge;
import tech.pegasys.teku.infrastructure.metrics.TekuMetricCategory;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.networking.eth2.Eth2Network;
import tech.pegasys.teku.ssz.SSZTypes.Bitlist;
import tech.pegasys.teku.ssz.SSZTypes.SSZList;
import tech.pegasys.teku.storage.client.RecentChainData;
import tech.pegasys.teku.util.time.channels.SlotEventsChannel;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static tech.pegasys.teku.datastructures.util.BeaconStateUtil.compute_epoch_at_slot;
import static tech.pegasys.teku.datastructures.util.BeaconStateUtil.get_current_epoch;
import static tech.pegasys.teku.datastructures.util.BeaconStateUtil.get_previous_epoch;
import static tech.pegasys.teku.datastructures.util.ValidatorsUtil.get_active_validator_indices;

public class BeaconChainMetrics implements SlotEventsChannel {
  private static final long NOT_SET = 0L;
  private final RecentChainData recentChainData;
  private final NodeSlot nodeSlot;

  private final SettableDoubleGauge previousLiveValidators;
  private final SettableDoubleGauge currentActiveValidators;
  private final SettableDoubleGauge previousActiveValidators;
  private final SettableDoubleGauge currentLiveValidators;
  private final SettableDoubleGauge finalizedEpoch;
  private final SettableDoubleGauge finalizedRoot;
  private final SettableDoubleGauge currentJustifiedEpoch;
  private final SettableDoubleGauge currentJustifiedRoot;
  private final SettableDoubleGauge previousJustifiedEpoch;
  private final SettableDoubleGauge previousJustifiedRoot;

  public BeaconChainMetrics(
      final RecentChainData recentChainData,
      final NodeSlot nodeSlot,
      final MetricsSystem metricsSystem,
      final Eth2Network p2pNetwork) {
    this.recentChainData = recentChainData;
    this.nodeSlot = nodeSlot;

    metricsSystem.createGauge(
        TekuMetricCategory.BEACON,
        "epoch",
        "Latest epoch recorded by the beacon chain",
        this::getCurrentEpochValue);
    metricsSystem.createGauge(
        TekuMetricCategory.BEACON,
        "slot",
        "Latest slot recorded by the beacon chain",
        this::getCurrentSlotValue);
    metricsSystem.createGauge(
        TekuMetricCategory.BEACON,
        "head_slot",
        "Slot of the head block of the beacon chain",
        this::getHeadSlotValue);
    metricsSystem.createGauge(
        TekuMetricCategory.BEACON,
        "head_root",
        "Root of the head block of the beacon chain",
        this::getHeadRootValue);
    metricsSystem.createGauge(
        TekuMetricCategory.BEACON,
        "peer_count",
        "Tracks number of connected peers, verified to be on the same chain",
        p2pNetwork::getPeerCount);

    finalizedEpoch =
        SettableDoubleGauge.create(
            metricsSystem, TekuMetricCategory.BEACON, "finalized_epoch", "Current finalized epoch");
    finalizedRoot =
        SettableDoubleGauge.create(
            metricsSystem, TekuMetricCategory.BEACON, "finalized_root", "Current finalized root");

    currentJustifiedEpoch =
        SettableDoubleGauge.create(
            metricsSystem,
            TekuMetricCategory.BEACON,
            "current_justified_epoch",
            "Current justified epoch");
    currentJustifiedRoot =
        SettableDoubleGauge.create(
            metricsSystem,
            TekuMetricCategory.BEACON,
            "current_justified_root",
            "Current justified root");

    previousJustifiedEpoch =
        SettableDoubleGauge.create(
            metricsSystem,
            TekuMetricCategory.BEACON,
            "previous_justified_epoch",
            "Current previously justified epoch");
    previousJustifiedRoot =
        SettableDoubleGauge.create(
            metricsSystem,
            TekuMetricCategory.BEACON,
            "previous_justified_root",
            "Current previously justified root");

    previousLiveValidators =
        SettableDoubleGauge.create(
            metricsSystem,
            TekuMetricCategory.BEACON,
            "previous_live_validators",
            "Number of active validators that successfully included attestation on chain for previous epoch");
    currentLiveValidators =
        SettableDoubleGauge.create(
            metricsSystem,
            TekuMetricCategory.BEACON,
            "current_live_validators",
            "Number of active validators that successfully included attestation on chain for current epoch");
    currentActiveValidators =
        SettableDoubleGauge.create(
            metricsSystem,
            TekuMetricCategory.BEACON,
            "current_active_validators",
            "Number of active validators in the current epoch");
    previousActiveValidators =
        SettableDoubleGauge.create(
            metricsSystem,
            TekuMetricCategory.BEACON,
            "previous_active_validators",
            "Number of active validators in the previous epoch");
  }

  @Override
  public void onSlot(final UInt64 slot) {
    recentChainData.getBestState().ifPresent(this::updateMetrics);
  }

  private void updateMetrics(final BeaconState state) {
    currentLiveValidators.set(getLiveValidators(state.getCurrent_epoch_attestations()));
    currentActiveValidators.set(
        get_active_validator_indices(state, get_current_epoch(state)).size());
    previousLiveValidators.set(getLiveValidators(state.getPrevious_epoch_attestations()));
    previousActiveValidators.set(
        get_active_validator_indices(state, get_previous_epoch(state)).size());

    final Checkpoint finalizedCheckpoint = state.getFinalized_checkpoint();
    finalizedEpoch.set(finalizedCheckpoint.getEpoch().longValue());
    finalizedRoot.set(getLongFromRoot(finalizedCheckpoint.getRoot()));

    final Checkpoint currentJustifiedCheckpoint = state.getCurrent_justified_checkpoint();
    currentJustifiedEpoch.set(currentJustifiedCheckpoint.getEpoch().longValue());
    currentJustifiedRoot.set(getLongFromRoot(currentJustifiedCheckpoint.getRoot()));

    final Checkpoint previousJustifiedCheckpoint = state.getPrevious_justified_checkpoint();
    previousJustifiedEpoch.set(previousJustifiedCheckpoint.getEpoch().longValue());
    previousJustifiedRoot.set(getLongFromRoot(previousJustifiedCheckpoint.getRoot()));
  }

  private int getLiveValidators(final SSZList<PendingAttestation> attestations) {
    final Map<UInt64, Map<UInt64, Bitlist>> aggregationBitsBySlotAndCommittee = new HashMap<>();
    attestations.forEach(
        attestation ->
            aggregationBitsBySlotAndCommittee
                .computeIfAbsent(attestation.getData().getSlot(), __ -> new HashMap<>())
                .computeIfAbsent(
                    attestation.getData().getIndex(),
                    __ -> attestation.getAggregation_bits().copy())
                .setAllBits(attestation.getAggregation_bits()));

    return aggregationBitsBySlotAndCommittee.values().stream()
        .flatMap(aggregationBitsByCommittee -> aggregationBitsByCommittee.values().stream())
        .mapToInt(Bitlist::getBitCount)
        .sum();
  }

  static long getLongFromRoot(Bytes32 root) {
    return root.getLong(24, ByteOrder.LITTLE_ENDIAN);
  }

  private long getCurrentSlotValue() {
    return nodeSlot.longValue();
  }

  private long getHeadSlotValue() {
    if (recentChainData.isPreGenesis()) {
      return NOT_SET;
    }
    return recentChainData.getHeadSlot().longValue();
  }

  private long getHeadRootValue() {
    if (recentChainData.isPreGenesis()) {
      return NOT_SET;
    }
    Optional<Bytes32> maybeBlockRoot = recentChainData.getBestBlockRoot();
    return maybeBlockRoot.map(BeaconChainMetrics::getLongFromRoot).orElse(0L);
  }

  private long getCurrentEpochValue() {
    return compute_epoch_at_slot(nodeSlot.getValue()).longValue();
  }
}
