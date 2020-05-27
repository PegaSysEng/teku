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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.UnsignedLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.datastructures.attestation.ProcessedAggregateConsumer;
import tech.pegasys.teku.datastructures.attestation.ProcessedAttestationConsumer;
import tech.pegasys.teku.datastructures.attestation.ValidateableAttestation;
import tech.pegasys.teku.datastructures.blocks.SignedBeaconBlock;
import tech.pegasys.teku.service.serviceutils.Service;
import tech.pegasys.teku.statetransition.events.block.ImportedBlockEvent;
import tech.pegasys.teku.statetransition.util.FutureItems;
import tech.pegasys.teku.statetransition.util.PendingPool;
import tech.pegasys.teku.util.async.SafeFuture;
import tech.pegasys.teku.util.events.Subscribers;
import tech.pegasys.teku.util.time.channels.SlotEventsChannel;

import java.util.List;

public class AttestationManager extends Service implements SlotEventsChannel {

  private static final Logger LOG = LogManager.getLogger();

  private final EventBus eventBus;
  private final ForkChoiceAttestationProcessor attestationProcessor;

  private final PendingPool<ValidateableAttestation> pendingAttestations;
  private final FutureItems<ValidateableAttestation> futureAttestations;
  private final AggregatingAttestationPool aggregatingAttestationPool;

  private final Subscribers<ProcessedAttestationConsumer> processedAttestationSubscriber = Subscribers.create(true);
  private final Subscribers<ProcessedAggregateConsumer> processedAggregateSubscriber = Subscribers.create(true);


  AttestationManager(
      final EventBus eventBus,
      final ForkChoiceAttestationProcessor attestationProcessor,
      final PendingPool<ValidateableAttestation> pendingAttestations,
      final FutureItems<ValidateableAttestation> futureAttestations,
      final AggregatingAttestationPool aggregatingAttestationPool) {
    this.eventBus = eventBus;
    this.attestationProcessor = attestationProcessor;
    this.pendingAttestations = pendingAttestations;
    this.futureAttestations = futureAttestations;
    this.aggregatingAttestationPool = aggregatingAttestationPool;
  }

  public static AttestationManager create(
      final EventBus eventBus,
      final PendingPool<ValidateableAttestation> pendingAttestations,
      final FutureItems<ValidateableAttestation> futureAttestations,
      final ForkChoiceAttestationProcessor forkChoiceAttestationProcessor,
      final AggregatingAttestationPool aggregatingAttestationPool) {
    return new AttestationManager(
        eventBus,
        forkChoiceAttestationProcessor,
        pendingAttestations,
        futureAttestations,
        aggregatingAttestationPool);
  }

  public void subscribeToProcessedAttestations(ProcessedAttestationConsumer processedAttestationConsumer) {
    processedAttestationSubscriber.subscribe(processedAttestationConsumer);
  }

  public void subscribeToProcessedAggregates(ProcessedAggregateConsumer processedAggregateConsumer) {
    processedAggregateSubscriber.subscribe(processedAggregateConsumer);
  }

  @Override
  public void onSlot(final UnsignedLong slot) {
    List<ValidateableAttestation> attestations = futureAttestations.prune(slot);
    attestations
            .stream()
            .map(ValidateableAttestation::getIndexedAttestation)
            .forEach(attestationProcessor::applyIndexedAttestationToForkChoice);

    attestations.forEach(this::gossipAttestationIfNeeded);
  }

  private void gossipAttestationIfNeeded(ValidateableAttestation attestation) {
    if (attestation.isGossiped()) {
      return;
    }

    if (attestation.isAggregate()) {
      processedAggregateSubscriber.forEach(s -> s.accept(attestation));
    } else {
      processedAttestationSubscriber.forEach(s -> s.accept(attestation));
    }
  }

  @Subscribe
  @SuppressWarnings("unused")
  private void onBlockImported(final ImportedBlockEvent blockImportedEvent) {
    final SignedBeaconBlock block = blockImportedEvent.getBlock();
    final Bytes32 blockRoot = block.getMessage().hash_tree_root();
    pendingAttestations
        .getItemsDependingOn(blockRoot, false)
        .forEach(
            attestation -> {
              pendingAttestations.remove(attestation);
              onAttestation(attestation);
            });
  }

  public void onAttestation(final ValidateableAttestation attestation) {
    if (pendingAttestations.contains(attestation)) {
      return;
    }

    switch (attestationProcessor.processAttestation(attestation)) {
      case SUCCESSFUL:
        LOG.trace("Processed attestation {} successfully", attestation::hash_tree_root);
        aggregatingAttestationPool.add(attestation);
        gossipAttestationIfNeeded(attestation);
        break;
      case UNKNOWN_BLOCK:
        LOG.trace(
            "Deferring attestation {} as required block is not yet present",
            attestation::hash_tree_root);
        pendingAttestations.add(attestation);
        break;
      case SAVED_FOR_FUTURE:
        LOG.trace(
            "Deferring attestation {} until a future slot", attestation::hash_tree_root);
        futureAttestations.add(attestation);
        aggregatingAttestationPool.add(attestation);
        break;
      case INVALID:
        break;
      default:
        throw new UnsupportedOperationException("AttestationProcessingResult is unrecognizable");
    }
  }

  @Override
  protected SafeFuture<?> doStart() {
    eventBus.register(this);
    return SafeFuture.COMPLETE;
  }

  @Override
  protected SafeFuture<?> doStop() {
    eventBus.unregister(this);
    return SafeFuture.COMPLETE;
  }
}
