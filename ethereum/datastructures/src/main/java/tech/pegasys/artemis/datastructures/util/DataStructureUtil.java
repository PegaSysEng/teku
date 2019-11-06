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

package tech.pegasys.artemis.datastructures.util;

import static java.lang.Math.toIntExact;
import static tech.pegasys.artemis.util.config.Constants.SLOTS_PER_ETH1_VOTING_PERIOD;

import com.google.common.primitives.UnsignedLong;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.LongStream;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.artemis.datastructures.blocks.BeaconBlock;
import tech.pegasys.artemis.datastructures.blocks.BeaconBlockBody;
import tech.pegasys.artemis.datastructures.blocks.BeaconBlockHeader;
import tech.pegasys.artemis.datastructures.blocks.Eth1Data;
import tech.pegasys.artemis.datastructures.operations.Attestation;
import tech.pegasys.artemis.datastructures.operations.AttestationData;
import tech.pegasys.artemis.datastructures.operations.AttesterSlashing;
import tech.pegasys.artemis.datastructures.operations.Deposit;
import tech.pegasys.artemis.datastructures.operations.DepositData;
import tech.pegasys.artemis.datastructures.operations.DepositWithIndex;
import tech.pegasys.artemis.datastructures.operations.IndexedAttestation;
import tech.pegasys.artemis.datastructures.operations.ProposerSlashing;
import tech.pegasys.artemis.datastructures.operations.VoluntaryExit;
import tech.pegasys.artemis.datastructures.state.BeaconState;
import tech.pegasys.artemis.datastructures.state.Checkpoint;
import tech.pegasys.artemis.datastructures.state.Fork;
import tech.pegasys.artemis.datastructures.state.PendingAttestation;
import tech.pegasys.artemis.datastructures.state.Validator;
import tech.pegasys.artemis.util.SSZTypes.Bitlist;
import tech.pegasys.artemis.util.SSZTypes.Bitvector;
import tech.pegasys.artemis.util.SSZTypes.Bytes4;
import tech.pegasys.artemis.util.SSZTypes.SSZList;
import tech.pegasys.artemis.util.SSZTypes.SSZVector;
import tech.pegasys.artemis.util.bls.BLSKeyPair;
import tech.pegasys.artemis.util.bls.BLSPublicKey;
import tech.pegasys.artemis.util.bls.BLSSignature;
import tech.pegasys.artemis.util.config.Constants;

public final class DataStructureUtil {

  public static int randomInt(int seed) {
    return new Random(seed).nextInt();
  }

  public static long randomLong(long seed) {
    return new Random(seed).nextLong();
  }

  public static UnsignedLong randomUnsignedLong(long seed) {
    return UnsignedLong.fromLongBits(randomLong(seed));
  }

  public static Bytes32 randomBytes32(long seed) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(seed);
    return Bytes32.random(new SecureRandom(buffer.array()));
  }

  public static <T> SSZList<T> randomSSZList(
      Class<T> classInfo, long maxSize, Function<Integer, T> randomFunction, int seed) {
    SSZList<T> sszList = new SSZList<>(classInfo, maxSize);
    long numItems = maxSize / 10;
    LongStream.range(0, numItems).forEach(i -> sszList.add(randomFunction.apply(seed)));
    return sszList;
  }

  public static <T> SSZVector<T> randomSSZVector(
      T defaultClassObject, long maxSize, Function<Integer, T> randomFunction, int seed) {
    SSZVector<T> sszvector = new SSZVector<>(toIntExact(maxSize), defaultClassObject);
    long numItems = maxSize / 10;
    LongStream.range(0, numItems)
        .forEach(i -> sszvector.set(toIntExact(i), randomFunction.apply(seed)));
    return sszvector;
  }

  public static Bitlist randomBitlist(long seed) {
    return randomBitlist(Constants.MAX_VALIDATORS_PER_COMMITTEE, seed);
  }

  public static Bitlist randomBitlist(int n, long seed) {
    byte[] byteArray = new byte[n];
    Random random = new Random(seed);

    for (int i = 0; i < n; i++) {
      byteArray[i] = (byte) (random.nextBoolean() ? 1 : 0);
    }
    return new Bitlist(byteArray, n);
  }

  public static Bitvector randomBitvector(int n, long seed) {
    byte[] byteArray = new byte[n];
    Random random = new Random(seed);

    for (int i = 0; i < n; i++) {
      byteArray[i] = (byte) (random.nextBoolean() ? 1 : 0);
    }
    return new Bitvector(byteArray, n);
  }

  public static BLSPublicKey randomPublicKey(int seed) {
    return BLSPublicKey.random(seed);
  }

  public static Eth1Data randomEth1Data(int seed) {
    return new Eth1Data(randomBytes32(seed), randomUnsignedLong(seed++), randomBytes32(seed++));
  }

  public static Checkpoint randomCheckpoint(int seed) {
    return new Checkpoint(randomUnsignedLong(seed), randomBytes32(seed++));
  }

  public static AttestationData randomAttestationData(int seed) {
    return new AttestationData(
        randomUnsignedLong(seed),
        randomUnsignedLong(seed),
        randomBytes32(seed),
        randomCheckpoint(seed++),
        randomCheckpoint(seed++));
  }

  public static Attestation randomAttestation(int seed) {
    return new Attestation(
        randomBitlist(seed),
        randomAttestationData(seed++),
        randomBitlist(seed++),
        BLSSignature.random(seed++));
  }

  public static PendingAttestation randomPendingAttestation(int seed) {
    return new PendingAttestation(
        randomBitlist(seed),
        randomAttestationData(seed++),
        randomUnsignedLong(seed++),
        randomUnsignedLong(seed++));
  }

  public static AttesterSlashing randomAttesterSlashing(int seed) {
    return new AttesterSlashing(randomIndexedAttestation(seed), randomIndexedAttestation(seed++));
  }

  public static BeaconBlock randomBeaconBlock(long slotNum, int seed) {
    UnsignedLong slot = UnsignedLong.valueOf(slotNum);
    Bytes32 previous_root = randomBytes32(seed);
    Bytes32 state_root = randomBytes32(seed++);
    BeaconBlockBody body = randomBeaconBlockBody(seed++);
    BLSSignature signature = BLSSignature.random(seed++);

    return new BeaconBlock(slot, previous_root, state_root, body, signature);
  }

  public static BeaconBlockHeader randomBeaconBlockHeader(int seed) {
    return new BeaconBlockHeader(
        randomUnsignedLong(seed++),
        randomBytes32(seed++),
        randomBytes32(seed++),
        randomBytes32(seed++),
        BLSSignature.random(seed));
  }

  public static BeaconBlockBody randomBeaconBlockBody(int seed) {
    return new BeaconBlockBody(
        BLSSignature.random(seed),
        randomEth1Data(seed++),
        Bytes32.ZERO,
        randomSSZList(
            ProposerSlashing.class,
            Constants.MAX_PROPOSER_SLASHINGS,
            DataStructureUtil::randomProposerSlashing,
            seed++),
        randomSSZList(
            AttesterSlashing.class,
            Constants.MAX_ATTESTER_SLASHINGS,
            DataStructureUtil::randomAttesterSlashing,
            seed++),
        randomSSZList(
            Attestation.class,
            Constants.MAX_ATTESTATIONS,
            DataStructureUtil::randomAttestation,
            seed++),
        randomSSZList(
            Deposit.class,
            Constants.MAX_DEPOSITS,
            DataStructureUtil::randomDepositWithoutIndex,
            seed++),
        randomSSZList(
            VoluntaryExit.class,
            Constants.MAX_VOLUNTARY_EXITS,
            DataStructureUtil::randomVoluntaryExit,
            seed++));
  }

  public static ProposerSlashing randomProposerSlashing(int seed) {
    return new ProposerSlashing(
        randomUnsignedLong(seed++), randomBeaconBlockHeader(seed++), randomBeaconBlockHeader(seed));
  }

  public static IndexedAttestation randomIndexedAttestation(int seed) {
    SSZList<UnsignedLong> custody_0_bit_indices =
        new SSZList<>(UnsignedLong.class, Constants.MAX_VALIDATORS_PER_COMMITTEE);
    SSZList<UnsignedLong> custody_1_bit_indices =
        new SSZList<>(UnsignedLong.class, Constants.MAX_VALIDATORS_PER_COMMITTEE);
    custody_0_bit_indices.add(randomUnsignedLong(seed));
    custody_0_bit_indices.add(randomUnsignedLong(seed++));
    custody_0_bit_indices.add(randomUnsignedLong(seed++));
    custody_1_bit_indices.add(randomUnsignedLong(seed++));
    custody_1_bit_indices.add(randomUnsignedLong(seed++));
    custody_1_bit_indices.add(randomUnsignedLong(seed++));
    return new IndexedAttestation(
        custody_0_bit_indices,
        custody_1_bit_indices,
        randomAttestationData(seed++),
        BLSSignature.random(seed++));
  }

  public static DepositData randomDepositData(int seed) {
    BLSKeyPair keyPair = BLSKeyPair.random(seed);
    BLSPublicKey pubkey = keyPair.getPublicKey();
    Bytes32 withdrawal_credentials = randomBytes32(seed++);

    DepositData proof_of_possession_data =
        new DepositData(
            pubkey,
            withdrawal_credentials,
            UnsignedLong.valueOf(Constants.MAX_EFFECTIVE_BALANCE),
            Constants.EMPTY_SIGNATURE);

    BLSSignature proof_of_possession =
        BLSSignature.sign(
            keyPair,
            proof_of_possession_data.signing_root("signature"),
            BeaconStateUtil.compute_domain(Constants.DOMAIN_DEPOSIT));

    return new DepositData(
        keyPair.getPublicKey(),
        withdrawal_credentials,
        UnsignedLong.valueOf(Constants.MAX_EFFECTIVE_BALANCE),
        proof_of_possession);
  }

  public static DepositWithIndex randomDepositWithIndex(int seed) {
    return new DepositWithIndex(
        new SSZVector<>(32, randomBytes32(seed)),
        randomDepositData(seed++),
        randomUnsignedLong(seed++)
            .mod(UnsignedLong.valueOf(Constants.DEPOSIT_CONTRACT_TREE_DEPTH)));
  }

  public static Deposit randomDepositWithoutIndex(int seed) {
    return new Deposit(
        new SSZVector<>(Constants.DEPOSIT_CONTRACT_TREE_DEPTH + 1, randomBytes32(seed)),
        randomDepositData(seed++));
  }

  public static Deposit randomDeposit(int seed) {
    return new Deposit(
        new SSZVector<>(Constants.DEPOSIT_CONTRACT_TREE_DEPTH + 1, randomBytes32(seed)),
        randomDepositData(seed));
  }

  public static ArrayList<DepositWithIndex> randomDeposits(int num, int seed) {
    ArrayList<DepositWithIndex> deposits = new ArrayList<>();

    for (int i = 0; i < num; i++) {
      deposits.add(randomDepositWithIndex(seed++));
    }

    return deposits;
  }

  public static VoluntaryExit randomVoluntaryExit(int seed) {
    return new VoluntaryExit(
        randomUnsignedLong(seed), randomUnsignedLong(seed++), BLSSignature.random(seed++));
  }

  public static ArrayList<DepositWithIndex> newDeposits(int numDeposits) {
    ArrayList<DepositWithIndex> deposits = new ArrayList<>();

    for (int i = 0; i < numDeposits; i++) {
      BLSKeyPair keypair = BLSKeyPair.random(i);
      DepositData depositData =
          new DepositData(
              keypair.getPublicKey(),
              Bytes32.ZERO,
              UnsignedLong.valueOf(Constants.MAX_EFFECTIVE_BALANCE),
              BLSSignature.empty());
      BLSSignature proof_of_possession =
          BLSSignature.sign(
              keypair,
              depositData.signing_root("signature"),
              BeaconStateUtil.compute_domain(Constants.DOMAIN_DEPOSIT));
      depositData.setSignature(proof_of_possession);

      SSZVector<Bytes32> proof =
          new SSZVector<>(Constants.DEPOSIT_CONTRACT_TREE_DEPTH + 1, Bytes32.ZERO);
      DepositWithIndex deposit = new DepositWithIndex(proof, depositData, UnsignedLong.valueOf(i));
      deposits.add(deposit);
    }
    return deposits;
  }

  public static Validator randomValidator(int seed) {
    return new Validator(
        randomPublicKey(seed),
        randomBytes32(seed++),
        UnsignedLong.valueOf(Constants.MAX_EFFECTIVE_BALANCE),
        false,
        Constants.FAR_FUTURE_EPOCH,
        Constants.FAR_FUTURE_EPOCH,
        Constants.FAR_FUTURE_EPOCH,
        Constants.FAR_FUTURE_EPOCH);
  }

  public static Fork randomFork(int seed) {
    return new Fork(
        new Bytes4(randomBytes32(seed).slice(0, 4)),
        new Bytes4(randomBytes32(seed++).slice(0, 4)),
        randomUnsignedLong(seed++));
  }

  public static BeaconState randomBeaconState(int seed) {
    return new BeaconState(
        randomUnsignedLong(seed),
        randomUnsignedLong(seed++),
        randomFork(seed++),
        randomBeaconBlockHeader(seed++),
        randomSSZVector(
            Bytes32.ZERO,
            Constants.SLOTS_PER_HISTORICAL_ROOT,
            DataStructureUtil::randomBytes32,
            seed++),
        randomSSZVector(
            Bytes32.ZERO,
            Constants.SLOTS_PER_HISTORICAL_ROOT,
            DataStructureUtil::randomBytes32,
            seed++),
        randomSSZList(Bytes32.class, 1000, DataStructureUtil::randomBytes32, seed++),
        randomEth1Data(seed++),
        randomSSZList(
            Eth1Data.class,
            SLOTS_PER_ETH1_VOTING_PERIOD,
            DataStructureUtil::randomEth1Data,
            seed++),
        randomUnsignedLong(seed++),

        // Can't use the actual maxSize cause it is too big
        randomSSZList(Validator.class, 1000, DataStructureUtil::randomValidator, seed++),
        randomSSZList(UnsignedLong.class, 1000, DataStructureUtil::randomUnsignedLong, seed++),
        randomSSZVector(
            Bytes32.ZERO,
            Constants.EPOCHS_PER_HISTORICAL_VECTOR,
            DataStructureUtil::randomBytes32,
            seed++),
        randomSSZVector(
            UnsignedLong.ZERO,
            Constants.EPOCHS_PER_SLASHINGS_VECTOR,
            DataStructureUtil::randomUnsignedLong,
            seed++),
        randomSSZList(
            PendingAttestation.class, 1000, DataStructureUtil::randomPendingAttestation, seed++),
        randomSSZList(
            PendingAttestation.class, 1000, DataStructureUtil::randomPendingAttestation, seed++),
        randomBitvector(Constants.JUSTIFICATION_BITS_LENGTH, seed++),
        randomCheckpoint(seed++),
        randomCheckpoint(seed++),
        randomCheckpoint(seed++));
  }

  public static BeaconState randomBeaconState(UnsignedLong slot, int seed) {
    BeaconState randomBeaconState = randomBeaconState(seed);
    randomBeaconState.setSlot(slot);
    return randomBeaconState;
  }
}
