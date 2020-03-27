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

package tech.pegasys.artemis.api;

import static com.google.common.primitives.UnsignedLong.ONE;
import static com.google.common.primitives.UnsignedLong.ZERO;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tech.pegasys.artemis.datastructures.util.BeaconStateUtil.compute_epoch_at_slot;

import com.google.common.primitives.UnsignedLong;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tech.pegasys.artemis.api.schema.Attestation;
import tech.pegasys.artemis.api.schema.BLSPubKey;
import tech.pegasys.artemis.api.schema.BLSSignature;
import tech.pegasys.artemis.api.schema.BeaconBlock;
import tech.pegasys.artemis.api.schema.BeaconState;
import tech.pegasys.artemis.api.schema.ValidatorDuties;
import tech.pegasys.artemis.api.schema.ValidatorDutiesRequest;
import tech.pegasys.artemis.datastructures.util.DataStructureUtil;
import tech.pegasys.artemis.storage.ChainDataUnavailableException;
import tech.pegasys.artemis.storage.CombinedChainDataClient;
import tech.pegasys.artemis.util.async.SafeFuture;
import tech.pegasys.artemis.util.bls.BLSPublicKey;
import tech.pegasys.artemis.validator.api.ValidatorApiChannel;

public class ValidatorDataProviderTest {

  private final ArgumentCaptor<tech.pegasys.artemis.datastructures.operations.Attestation> args =
      ArgumentCaptor.forClass(tech.pegasys.artemis.datastructures.operations.Attestation.class);

  private final DataStructureUtil dataStructureUtil = new DataStructureUtil();
  private CombinedChainDataClient combinedChainDataClient = mock(CombinedChainDataClient.class);
  private final ValidatorApiChannel validatorApiChannel = mock(ValidatorApiChannel.class);
  private ValidatorDataProvider provider =
      new ValidatorDataProvider(validatorApiChannel, combinedChainDataClient);
  private final tech.pegasys.artemis.datastructures.blocks.BeaconBlock blockInternal =
      dataStructureUtil.randomBeaconBlock(123);
  private final BeaconBlock block = new BeaconBlock(blockInternal);
  private final tech.pegasys.artemis.util.bls.BLSSignature signatureInternal =
      tech.pegasys.artemis.util.bls.BLSSignature.random(1234);
  private final BLSSignature signature = new BLSSignature(signatureInternal);
  private final tech.pegasys.artemis.datastructures.state.BeaconState beaconStateInternal =
      dataStructureUtil.randomBeaconState();
  private final BeaconState beaconState = new BeaconState(beaconStateInternal);

  @Test
  void getUnsignedBeaconBlockAtSlot_throwsWithoutSlotDefined() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> provider.getUnsignedBeaconBlockAtSlot(null, null));
  }

  @Test
  void getUnsignedBeaconBlockAtSlot_shouldThrowWithoutRandaoDefined() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> provider.getUnsignedBeaconBlockAtSlot(ONE, null));
  }

  @Test
  void getUnsignedBeaconBlockAtSlot_shouldCreateAnUnsignedBlock() {
    when(validatorApiChannel.createUnsignedBlock(ONE, signatureInternal))
        .thenReturn(SafeFuture.completedFuture(Optional.of(blockInternal)));

    SafeFuture<Optional<BeaconBlock>> data = provider.getUnsignedBeaconBlockAtSlot(ONE, signature);
    verify(validatorApiChannel).createUnsignedBlock(ONE, signatureInternal);
    assertThat(data).isCompleted();
    assertThat(data.getNow(null).orElseThrow()).usingRecursiveComparison().isEqualTo(block);
  }

  @Test
  void getUnsignedAttestationAtSlot_shouldThrowIfStoreNotFound() {
    when(combinedChainDataClient.isStoreAvailable()).thenReturn(false);
    final SafeFuture<Optional<Attestation>> result =
        provider.createUnsignedAttestationAtSlot(ZERO, 0);
    assertThat(result).isCompletedExceptionally();
    assertThatThrownBy(result::join).hasRootCauseInstanceOf(ChainDataUnavailableException.class);
    verify(combinedChainDataClient).isStoreAvailable();
  }

  @Test
  void getUnsignedAttestationAtSlot_shouldReturnEmptyIfBlockNotFound() {
    when(combinedChainDataClient.isStoreAvailable()).thenReturn(true);
    when(validatorApiChannel.createUnsignedAttestation(ZERO, 0))
        .thenReturn(SafeFuture.completedFuture(Optional.empty()));

    final SafeFuture<Optional<Attestation>> result =
        provider.createUnsignedAttestationAtSlot(ZERO, 0);
    verify(validatorApiChannel).createUnsignedAttestation(ZERO, 0);
    assertThat(result).isCompletedWithValue(Optional.empty());
  }

  @Test
  void getUnsignedAttestationAtSlot_shouldReturnAttestation() throws Exception {
    when(combinedChainDataClient.isStoreAvailable()).thenReturn(true);
    final tech.pegasys.artemis.datastructures.operations.Attestation internalAttestation =
        dataStructureUtil.randomAttestation();
    when(validatorApiChannel.createUnsignedAttestation(ONE, 0))
        .thenReturn(SafeFuture.completedFuture(Optional.of(internalAttestation)));

    final SafeFuture<Optional<Attestation>> result =
        provider.createUnsignedAttestationAtSlot(ONE, 0);
    assertThat(result).isCompleted();
    Attestation attestation = result.join().orElseThrow();
    assertThat(attestation.data.index).isEqualTo(internalAttestation.getData().getIndex());
    assertThat(attestation.signature.toHexString())
        .isEqualTo(internalAttestation.getAggregate_signature().toBytes().toHexString());
    assertThat(attestation.data.slot).isEqualTo(internalAttestation.getData().getSlot());
    assertThat(attestation.data.beacon_block_root)
        .isEqualTo(internalAttestation.getData().getBeacon_block_root());
  }

  @Test
  void getValidatorsDutiesByRequest_shouldIncludeMissingValidators()
      throws ExecutionException, InterruptedException {
    when(combinedChainDataClient.isStoreAvailable()).thenReturn(true);
    final BLSPublicKey publicKey = dataStructureUtil.randomPublicKey();
    ValidatorDutiesRequest smallRequest =
        new ValidatorDutiesRequest(
            compute_epoch_at_slot(beaconState.slot),
            List.of(new BLSPubKey(publicKey.toBytesCompressed())));
    when(validatorApiChannel.getDuties(smallRequest.epoch, List.of(publicKey)))
        .thenReturn(
            SafeFuture.completedFuture(
                List.of(tech.pegasys.artemis.validator.api.ValidatorDuties.noDuties(publicKey))));

    SafeFuture<List<ValidatorDuties>> future = provider.getValidatorDutiesByRequest(smallRequest);
    List<ValidatorDuties> validatorDuties = future.get();

    assertThat(validatorDuties.size()).isEqualTo(1);
    ValidatorDuties expected =
        new ValidatorDuties(
            new BLSPubKey(publicKey.toBytesCompressed()), null, null, emptyList(), null);
    assertThat(validatorDuties.get(0)).isEqualToComparingFieldByField(expected);
  }

  @Test
  void getValidatorsDutiesByRequest_shouldThrowIllegalArgumentExceptionIfKeyIsNotOnTheCurve() {
    when(combinedChainDataClient.isStoreAvailable()).thenReturn(true);
    final BLSPublicKey publicKey = dataStructureUtil.randomPublicKey();
    // modify the bytes to make an invalid key that is the correct length
    final BLSPubKey invalidPubKey = new BLSPubKey(publicKey.toBytes().shiftLeft(1));

    ValidatorDutiesRequest smallRequest =
        new ValidatorDutiesRequest(compute_epoch_at_slot(beaconState.slot), List.of(invalidPubKey));
    when(validatorApiChannel.getDuties(smallRequest.epoch, List.of(publicKey)))
        .thenReturn(
            SafeFuture.completedFuture(
                List.of(tech.pegasys.artemis.validator.api.ValidatorDuties.noDuties(publicKey))));

    SafeFuture<List<ValidatorDuties>> future = provider.getValidatorDutiesByRequest(smallRequest);

    assertThatThrownBy(() -> future.get()).hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void getValidatorDutiesByRequest_shouldIncludeValidatorDuties()
      throws ExecutionException, InterruptedException {
    when(combinedChainDataClient.isStoreAvailable()).thenReturn(true);
    final BLSPublicKey publicKey = dataStructureUtil.randomPublicKey();
    ValidatorDutiesRequest smallRequest =
        new ValidatorDutiesRequest(
            compute_epoch_at_slot(beaconState.slot),
            List.of(new BLSPubKey(publicKey.toBytesCompressed())));
    final int validatorIndex = 4;
    final int attestationCommitteeIndex = 2;
    final List<UnsignedLong> blockProposalSlots =
        List.of(UnsignedLong.valueOf(66), UnsignedLong.valueOf(77));
    final UnsignedLong attestationSlot = UnsignedLong.valueOf(50);
    when(validatorApiChannel.getDuties(smallRequest.epoch, List.of(publicKey)))
        .thenReturn(
            SafeFuture.completedFuture(
                List.of(
                    tech.pegasys.artemis.validator.api.ValidatorDuties.withDuties(
                        publicKey,
                        validatorIndex,
                        attestationCommitteeIndex,
                        blockProposalSlots,
                        attestationSlot))));

    SafeFuture<List<ValidatorDuties>> future = provider.getValidatorDutiesByRequest(smallRequest);
    List<ValidatorDuties> validatorDuties = future.get();

    assertThat(validatorDuties.size()).isEqualTo(1);
    ValidatorDuties expected =
        new ValidatorDuties(
            new BLSPubKey(publicKey.toBytesCompressed()),
            validatorIndex,
            attestationCommitteeIndex,
            blockProposalSlots,
            attestationSlot);
    assertThat(validatorDuties.get(0)).isEqualToComparingFieldByField(expected);
  }

  @Test
  void getValidatorDutiesByRequest_shouldReturnChainDataUnavailableExceptionWhenStoreIsNotSet() {
    when(combinedChainDataClient.isStoreAvailable()).thenReturn(false);

    final SafeFuture<List<ValidatorDuties>> result =
        provider.getValidatorDutiesByRequest(new ValidatorDutiesRequest(ONE, emptyList()));

    assertThat(result).isCompletedExceptionally();
    assertThatThrownBy(result::join).hasRootCauseInstanceOf(ChainDataUnavailableException.class);
  }

  @Test
  void submitAttestation_shouldSubmitAnInternalAttestationStructure() {
    tech.pegasys.artemis.datastructures.operations.Attestation internalAttestation =
        dataStructureUtil.randomAttestation();
    Attestation attestation = new Attestation(internalAttestation);

    provider.submitAttestation(attestation);

    verify(validatorApiChannel).sendSignedAttestation(args.capture());
    assertThat(args.getValue()).usingRecursiveComparison().isEqualTo(internalAttestation);
  }
}
