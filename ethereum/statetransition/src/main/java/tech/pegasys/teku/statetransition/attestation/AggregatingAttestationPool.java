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

package tech.pegasys.teku.statetransition.attestation;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.core.operationvalidators.AttestationDataStateTransitionValidator;
import tech.pegasys.teku.datastructures.attestation.ValidateableAttestation;
import tech.pegasys.teku.datastructures.blocks.BeaconBlockBodyLists;
import tech.pegasys.teku.datastructures.operations.Attestation;
import tech.pegasys.teku.datastructures.operations.AttestationData;
import tech.pegasys.teku.datastructures.state.BeaconState;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.ssz.SSZTypes.SSZList;
import tech.pegasys.teku.ssz.SSZTypes.SSZMutableList;
import tech.pegasys.teku.util.time.channels.SlotEventsChannel;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static tech.pegasys.teku.util.config.Constants.ATTESTATION_RETENTION_EPOCHS;
import static tech.pegasys.teku.util.config.Constants.SLOTS_PER_EPOCH;

/**
 * Maintains a pool of attestations. Attestations can be retrieved either for inclusion in a block
 * or as an aggregate to publish as part of the naive attestation aggregation algorithm. In both
 * cases the returned attestations are aggregated to maximise the number of validators that can be
 * included.
 */
public class AggregatingAttestationPool implements SlotEventsChannel {

  private final Map<Bytes, MatchingDataAttestationGroup> attestationGroupByDataHash =
      new HashMap<>();
  private final NavigableMap<UInt64, Set<Bytes>> dataHashBySlot = new TreeMap<>();
  private final AttestationDataStateTransitionValidator attestationDataValidator;
  private final AtomicInteger size = new AtomicInteger(0);

  public AggregatingAttestationPool(
      final AttestationDataStateTransitionValidator attestationDataValidator) {
    this.attestationDataValidator = attestationDataValidator;
  }

  public synchronized void add(final ValidateableAttestation attestation) {
    final AttestationData attestationData = attestation.getAttestation().getData();
    final Bytes32 dataRoot = attestationData.hash_tree_root();
    final boolean add =
        attestationGroupByDataHash
            .computeIfAbsent(
                dataRoot,
                key ->
                    new MatchingDataAttestationGroup(
                        attestationData,
                        attestation
                            .getCommitteeShufflingSeed()
                            .orElseThrow(
                                () ->
                                    new UnsupportedOperationException(
                                        "ValidateableAttestation does not have a randao mix."))))
            .add(attestation);
    if (add) {
      size.incrementAndGet();
    }
    dataHashBySlot
        .computeIfAbsent(attestationData.getSlot(), slot -> new HashSet<>())
        .add(dataRoot);
  }

  @Override
  public synchronized void onSlot(final UInt64 slot) {
    final UInt64 attestationRetentionSlots =
        UInt64.valueOf(SLOTS_PER_EPOCH * ATTESTATION_RETENTION_EPOCHS);
    if (slot.compareTo(attestationRetentionSlots) <= 0) {
      return;
    }
    final UInt64 firstValidAttestationSlot = slot.minus(attestationRetentionSlots);
    final Collection<Set<Bytes>> dataHashesToRemove =
        dataHashBySlot.headMap(firstValidAttestationSlot, false).values();
    dataHashesToRemove.stream().flatMap(Set::stream).forEach(attestationGroupByDataHash::remove);
    dataHashesToRemove.clear();
  }

  public void removeAll(SSZList<Attestation> attestations) {
    attestations.forEach(this::remove);
  }

  public synchronized void remove(final Attestation attestation) {
    final AttestationData attestationData = attestation.getData();
    final Bytes32 dataRoot = attestationData.hash_tree_root();
    final MatchingDataAttestationGroup attestations = attestationGroupByDataHash.get(dataRoot);
    if (attestations == null) {
      return;
    }
    final int numRemoved = attestations.remove(attestation);
    size.addAndGet(-numRemoved);
    if (attestations.isEmpty()) {
      attestationGroupByDataHash.remove(dataRoot);
      removeFromSlotMappings(attestationData.getSlot(), dataRoot);
    }
  }

  private void removeFromSlotMappings(final UInt64 slot, final Bytes32 dataRoot) {
    final Set<Bytes> dataHashesForSlot = dataHashBySlot.get(slot);
    if (dataHashesForSlot != null) {
      dataHashesForSlot.remove(dataRoot);
      if (dataHashesForSlot.isEmpty()) {
        dataHashBySlot.remove(slot);
      }
    }
  }

  public int getSize() {
    return size.get();
  }

  public synchronized SSZList<Attestation> getAttestationsForBlock(
      final BeaconState stateAtBlockSlot, final AttestationForkChecker forkChecker) {
    final SSZMutableList<Attestation> attestations = BeaconBlockBodyLists.createAttestations();

    dataHashBySlot.descendingMap().values().stream()
        .flatMap(Collection::stream)
        .map(attestationGroupByDataHash::get)
        .filter(Objects::nonNull)
        .filter(group -> isValid(stateAtBlockSlot, group.getAttestationData()))
        .filter(forkChecker::areAttestationsFromCorrectFork)
        .flatMap(MatchingDataAttestationGroup::stream)
        .limit(attestations.getMaxSize())
        .map(ValidateableAttestation::getAttestation)
        .forEach(attestations::add);
    return attestations;
  }

  private boolean isValid(
      final BeaconState stateAtBlockSlot, final AttestationData attestationData) {
    return attestationDataValidator.validate(stateAtBlockSlot, attestationData).isEmpty();
  }

  public synchronized Optional<ValidateableAttestation> createAggregateFor(
      final Bytes32 attestationHashTreeRoot) {
    return Optional.ofNullable(attestationGroupByDataHash.get(attestationHashTreeRoot))
        .flatMap(attestations -> attestations.stream().findFirst());
  }
}
