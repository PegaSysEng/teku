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

package tech.pegasys.artemis.validator.client.duties;

import static tech.pegasys.artemis.datastructures.util.BeaconStateUtil.compute_epoch_at_slot;

import com.google.common.primitives.UnsignedLong;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.pegasys.artemis.datastructures.blocks.BeaconBlock;
import tech.pegasys.artemis.datastructures.blocks.SignedBeaconBlock;
import tech.pegasys.artemis.datastructures.state.Fork;
import tech.pegasys.artemis.util.async.SafeFuture;
import tech.pegasys.artemis.util.bls.BLSSignature;
import tech.pegasys.artemis.validator.api.ValidatorApiChannel;
import tech.pegasys.artemis.validator.client.ForkProvider;
import tech.pegasys.artemis.validator.client.Validator;

public class BlockProductionDuty implements Duty {
  private static final Logger LOG = LogManager.getLogger();
  private final Validator validator;
  private final UnsignedLong slot;
  private final ForkProvider forkProvider;
  private final ValidatorApiChannel validatorApiChannel;

  public BlockProductionDuty(
      final Validator validator,
      final UnsignedLong slot,
      final ForkProvider forkProvider,
      final ValidatorApiChannel validatorApiChannel) {
    LOG.trace("Created");
    this.validator = validator;
    this.slot = slot;
    this.forkProvider = forkProvider;
    this.validatorApiChannel = validatorApiChannel;
  }

  @Override
  public void performDuty() {
    LOG.trace("Creating block for validator {} at slot {}", validator.getPublicKey(), slot);
    forkProvider
        .getFork()
        .thenCompose(this::produceBlock)
        .finish(
            () ->
                LOG.debug("Validator {} created block at slot {}", validator.getPublicKey(), slot),
            error ->
                LOG.error(
                    "Validator "
                        + validator.getPublicKey()
                        + " failed to create block at slot "
                        + slot,
                    error));
  }

  public SafeFuture<Void> produceBlock(final Fork fork) {
    return createRandaoReveal(fork)
        .thenCompose(this::createUnsignedBlock)
        .thenCompose(unsignedBlock -> signBlock(fork, unsignedBlock))
        .thenAccept(validatorApiChannel::sendSignedBlock);
  }

  public SafeFuture<Optional<BeaconBlock>> createUnsignedBlock(final BLSSignature randaoReveal) {
    return validatorApiChannel.createUnsignedBlock(slot, randaoReveal);
  }

  public SafeFuture<BLSSignature> createRandaoReveal(final Fork fork) {
    return validator.getSigner().createRandaoReveal(compute_epoch_at_slot(slot), fork);
  }

  public SafeFuture<SignedBeaconBlock> signBlock(
      final Fork fork, final Optional<BeaconBlock> unsignedBlock) {
    return validator
        .getSigner()
        .signBlock(unsignedBlock.orElseThrow(), fork)
        .thenApply(signature -> new SignedBeaconBlock(unsignedBlock.orElseThrow(), signature));
  }
}
