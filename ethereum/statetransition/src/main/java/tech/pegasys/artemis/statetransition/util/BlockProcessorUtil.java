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

package tech.pegasys.artemis.statetransition.util;

import static java.lang.Math.toIntExact;
import static tech.pegasys.artemis.datastructures.Constants.DEPOSIT_CONTRACT_TREE_DEPTH;
import static tech.pegasys.artemis.datastructures.Constants.DOMAIN_ATTESTATION;
import static tech.pegasys.artemis.datastructures.Constants.DOMAIN_EXIT;
import static tech.pegasys.artemis.datastructures.Constants.DOMAIN_PROPOSAL;
import static tech.pegasys.artemis.datastructures.Constants.EMPTY_SIGNATURE;
import static tech.pegasys.artemis.datastructures.Constants.MAX_ATTESTATIONS;
import static tech.pegasys.artemis.datastructures.Constants.MAX_ATTESTER_SLASHINGS;
import static tech.pegasys.artemis.datastructures.Constants.MAX_DEPOSITS;
import static tech.pegasys.artemis.datastructures.Constants.MAX_PROPOSER_SLASHINGS;
import static tech.pegasys.artemis.datastructures.Constants.MIN_ATTESTATION_INCLUSION_DELAY;
import static tech.pegasys.artemis.datastructures.Constants.EPOCH_LENGTH;
import static tech.pegasys.artemis.datastructures.Constants.ZERO_HASH;
import static tech.pegasys.artemis.statetransition.util.BeaconStateUtil.get_attestation_participants;
import static tech.pegasys.artemis.statetransition.util.BeaconStateUtil.get_bitfield_bit;
import static tech.pegasys.artemis.statetransition.util.BeaconStateUtil.get_block_root;
import static tech.pegasys.artemis.statetransition.util.BeaconStateUtil.get_crosslink_committees_at_slot;
import static tech.pegasys.artemis.statetransition.util.BeaconStateUtil.get_current_epoch;
import static tech.pegasys.artemis.statetransition.util.BeaconStateUtil.get_domain;
import static tech.pegasys.artemis.statetransition.util.BeaconStateUtil.get_entry_exit_effect_epoch;
import static tech.pegasys.artemis.statetransition.util.BeaconStateUtil.initiate_validator_exit;
import static tech.pegasys.artemis.statetransition.util.BeaconStateUtil.is_double_vote;
import static tech.pegasys.artemis.statetransition.util.BeaconStateUtil.is_surround_vote;
import static tech.pegasys.artemis.statetransition.util.BeaconStateUtil.penalize_validator;
import static tech.pegasys.artemis.statetransition.util.BeaconStateUtil.process_deposit;
import static tech.pegasys.artemis.statetransition.util.BeaconStateUtil.slot_to_epoch;
import static tech.pegasys.artemis.statetransition.util.BeaconStateUtil.verify_slashable_attestation;
import static tech.pegasys.artemis.statetransition.util.EpochProcessorUtil.get_epoch_start_slot;
import static tech.pegasys.artemis.statetransition.util.TreeHashUtil.hash_tree_root;
import static tech.pegasys.artemis.util.bls.BLSVerify.bls_verify;

import com.google.common.primitives.UnsignedLong;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.bytes.Bytes48;
import net.consensys.cava.crypto.Hash;
import tech.pegasys.artemis.datastructures.Constants;
import tech.pegasys.artemis.datastructures.blocks.BeaconBlock;
import tech.pegasys.artemis.datastructures.blocks.Eth1DataVote;
import tech.pegasys.artemis.datastructures.blocks.ProposalSignedData;
import tech.pegasys.artemis.datastructures.operations.Attestation;
import tech.pegasys.artemis.datastructures.operations.AttestationDataAndCustodyBit;
import tech.pegasys.artemis.datastructures.operations.AttesterSlashing;
import tech.pegasys.artemis.datastructures.operations.Deposit;
import tech.pegasys.artemis.datastructures.operations.Exit;
import tech.pegasys.artemis.datastructures.operations.ProposerSlashing;
import tech.pegasys.artemis.datastructures.operations.SlashableAttestation;
import tech.pegasys.artemis.datastructures.state.CrosslinkCommittee;
import tech.pegasys.artemis.datastructures.state.PendingAttestation;
import tech.pegasys.artemis.datastructures.state.Validator;
import tech.pegasys.artemis.statetransition.BeaconState;
import tech.pegasys.artemis.util.bls.Signature;

public class BlockProcessorUtil {

  /**
   * Spec:
   * https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#proposer-signature
   *
   * @param state
   * @param block
   */
  public static boolean verify_signature(BeaconState state, BeaconBlock block)
      throws IllegalStateException {
    // Let block_without_signature_root be the hash_tree_root of block where
    // block.signature is set
    // to EMPTY_SIGNATURE.
    block.setSignature(EMPTY_SIGNATURE);
    Bytes32 blockHash = hash_tree_root(block.toBytes());
    // Let proposal_root = hash_tree_root(ProposalSignedData(state.slot,
    // BEACON_CHAIN_SHARD_NUMBER,
    // block_without_signature_root)).
    ProposalSignedData signedData =
        new ProposalSignedData(state.getSlot(), Constants.BEACON_CHAIN_SHARD_NUMBER, blockHash);
    Bytes32 proposalRoot = signedData.getBlock_root();
    // Verify that
    // bls_verify(pubkey=state.validator_registry[get_beacon_proposer_index(state,
    // state.slot)].pubkey, message=proposal_root, signature=block.signature,
    // domain=get_domain(state.fork,
    // state.slot, DOMAIN_PROPOSAL)).
    int proposerIndex = BeaconStateUtil.get_beacon_proposer_index(state, state.getSlot());
    Bytes48 pubkey = state.getValidator_registry().get(proposerIndex).getPubkey();
    return bls_verify(
        pubkey,
        proposalRoot,
        block.getSignature(),
        UnsignedLong.valueOf(Constants.DOMAIN_PROPOSAL));
  }

  /**
   * Spec: https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#randao
   *
   * @param state
   * @param block
   */
  public static void verify_and_update_randao(BeaconState state, BeaconBlock block)
      throws IllegalStateException {
    // Let proposer = state.validator_registry[get_beacon_proposer_index(state, state.slot)].
    int proposerIndex = BeaconStateUtil.get_beacon_proposer_index(state, state.getSlot());
    Bytes48 pubkey = state.getValidator_registry().get(proposerIndex).getPubkey();
    // TODO: convert these values to UnsignedLong
    long epoch = BeaconStateUtil.get_current_epoch(state).longValue();
    Bytes32 epochBytes = Bytes32.wrap(Bytes.minimalBytes(epoch));
    // Verify that bls_verify(pubkey=proposer.pubkey,
    // message=int_to_bytes32(get_current_epoch(state)), signature=block.randao_reveal, domain=
    // get_domain(state.fork, get_current_epoch(state), DOMAIN_RANDAO)).
    // TODO: after v0.01 refactor constants no longer exists
    //    BLSVerify.bls_verify(pubkey, epochBytes, block.getRandao_reveal(),
    // Constants.DOMAIN_RANDAO);
    // state.latest_randao_mixes[get_current_epoch(state) % LATEST_RANDAO_MIXES_LENGTH] =
    // xor(get_randao_mix(state, get_current_epoch(state)), hash(block.randao_reveal))
    int index = toIntExact(epoch) % Constants.LATEST_RANDAO_MIXES_LENGTH;
    Bytes32 latest_randao_mixes = state.getLatest_randao_mixes().get(index);
    state.getLatest_randao_mixes().set(index, latest_randao_mixes.xor(Hash.keccak256(epochBytes)));
  }
  /**
   * https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#eth1-data
   *
   * @param state
   * @param block
   */
  public static void tally_eth1_receipt_root_vote(BeaconState state, BeaconBlock block) {
    /*
     Eth1 data
     If block.eth1_data equals eth1_data_vote.eth1_data for some eth1_data_vote
       in state.eth1_data_votes, set eth1_data_vote.vote_count += 1.
     Otherwise, append to state.eth1_data_votes
       a new Eth1DataVote(eth1_data=block.eth1_data, vote_count=1).
    */

    boolean exists = false;
    List<Eth1DataVote> votes = state.getEth1_data_votes();
    for (Eth1DataVote vote : votes) {
      if (block.getEth1_data().equals(vote.getEth1_data())) {
        UnsignedLong voteCount = vote.getVote_count().plus(UnsignedLong.ONE);
        vote.setVote_count(voteCount);
        exists = true;
        break;
      }
    }
    if (!exists) {
      votes.add(new Eth1DataVote(block.getEth1_data(), UnsignedLong.ONE));
    }
  }

  /**
   *
   * @param state
   * @param block
   */
  public static void proposer_slashing(BeaconState state, BeaconBlock block) {
    assert block.getBody().getProposer_slashings().size() <= MAX_PROPOSER_SLASHINGS;

    for (ProposerSlashing proposer_slashing : block.getBody().getProposer_slashings()) {
      Validator proposer = state.getValidator_registry().get(proposer_slashing.getProposer_index().intValue());

      assert proposer_slashing.getProposal_data_1().getSlot().equals(proposer_slashing.getProposal_data_2().getSlot());
      assert proposer_slashing.getProposal_data_1().getShard().equals(proposer_slashing.getProposal_data_2().getShard());
      assert proposer_slashing.getProposal_data_1().getBlock_root() !=
          proposer_slashing.getProposal_data_2().getBlock_root();

      assert proposer.getPenalized_epoch().compareTo(get_current_epoch(state)) > 0;

      assert bls_verify(proposer.getPubkey(), hash_tree_root(proposer_slashing.getProposal_data_1().toBytes()),
          proposer_slashing.getProposal_signature_1(), get_domain(state.getFork(),
              slot_to_epoch(proposer_slashing.getProposal_data_1().getSlot()), DOMAIN_PROPOSAL));
      assert bls_verify(proposer.getPubkey(), hash_tree_root(proposer_slashing.getProposal_data_2().toBytes()),
          proposer_slashing.getProposal_signature_2(), get_domain(state.getFork(),
              slot_to_epoch(proposer_slashing.getProposal_data_2().getSlot()), DOMAIN_PROPOSAL));

      penalize_validator(state, proposer_slashing.getProposer_index().intValue());
    }
  }

  /**
   *
   * @param state
   * @param block
   */
  public static void attester_slashing(BeaconState state, BeaconBlock block) {
    assert block.getBody().getAttester_slashings().size() <= MAX_ATTESTER_SLASHINGS;

    for (AttesterSlashing attester_slashing : block.getBody().getAttester_slashings()) {
      SlashableAttestation slashable_attestation_1 = attester_slashing.getSlashable_attestation_1();
      SlashableAttestation slashable_attestation_2 = attester_slashing.getSlashable_attestation_2();

      assert slashable_attestation_1.getData() != slashable_attestation_2.getData();
      assert is_double_vote(slashable_attestation_1.getData(), slashable_attestation_2.getData()) ||
          is_surround_vote(slashable_attestation_1.getData(), slashable_attestation_2.getData());

      assert verify_slashable_attestation(state, slashable_attestation_1);
      assert verify_slashable_attestation(state, slashable_attestation_2);

      ArrayList<Integer> slashable_indices = new ArrayList<>();
      for (UnsignedLong index : slashable_attestation_1.getValidator_indices()) {

        if (slashable_attestation_2.getValidator_indices().contains(index) &&
            state.getValidator_registry().get(index.intValue()).getPenalized_epoch().compareTo(get_current_epoch(state))
                > 0) {
          slashable_indices.add(index.intValue());
        }
      }

      assert slashable_indices.size() >= 1;
      for (int index : slashable_indices) {
        penalize_validator(state, index);
      }
    }
  }

  /**
   *
   * @param state
   * @param block
   */
  public static void attestations(BeaconState state, BeaconBlock block) {
    assert block.getBody().getAttestations().size() <= MAX_ATTESTATIONS;
    for (Attestation attestation : block.getBody().getAttestations()) {
      assert attestation.getData().getSlot().compareTo(state.getSlot()
          .minus(UnsignedLong.valueOf(MIN_ATTESTATION_INCLUSION_DELAY))) <= 0;
      assert state.getSlot().minus(UnsignedLong.valueOf(MIN_ATTESTATION_INCLUSION_DELAY))
          .compareTo(attestation.getData().getSlot().plus(UnsignedLong.valueOf(EPOCH_LENGTH))) < 0;

      if (slot_to_epoch(attestation.getData().getSlot().plus(UnsignedLong.valueOf(1)))
          .compareTo(get_current_epoch(state)) >= 0) {
        assert attestation.getData().getJustified_epoch().equals(state.getJustified_epoch());
      } else {
        assert attestation.getData().getJustified_epoch().equals(state.getPrevious_justified_epoch());
      }

      try {
        assert attestation.getData().getJustified_block_root() == get_block_root(state,
            get_epoch_start_slot(attestation.getData().getJustified_epoch()));
      } catch (Exception e) {
        // todo: throw error
      }

      assert attestation.getData().getLatest_crosslink_root() ==
          state.getLatest_crosslinks().get(attestation.getData().getShard().intValue()).getShard_block_root() ||
          attestation.getData().getShard_block_root() ==
              state.getLatest_crosslinks().get(attestation.getData().getShard().intValue()).getShard_block_root();

      assert verify_bitfields_and_aggregate_signature(attestation, state);

      assert attestation.getData().getShard_block_root() == ZERO_HASH; //[TO BE REMOVED IN PHASE 1]

      PendingAttestation pendingAttestation = new PendingAttestation(attestation.getData(),
          attestation.getAggregation_bitfield(), attestation.getCustody_bitfield(), state.getSlot());
      state.getLatest_attestations().add(pendingAttestation);
    }

  }

  /**
   * Helper function for attestations.
   * @param attestation
   * @param state
   * @return true if bitfields and aggregate signature verified. Otherwise, false.
   */
  private static boolean verify_bitfields_and_aggregate_signature(Attestation attestation, BeaconState state) {
    assert attestation.getCustody_bitfield() == 0x00 * attestation.getCustody_bitfield().size();  // [TO BE REMOVED IN PHASE 1]
    assert attestation.getAggregation_bitfield() != 0x00 * attestation.getAggregation_bitfield().size();

    ArrayList<List<Integer>> crosslink_committees = new ArrayList<>();
    for (CrosslinkCommittee crosslink_committee : get_crosslink_committees_at_slot(state, attestation.getData().getSlot())) {
      if (crosslink_committee.getShard() == attestation.getData().getShard()) {
        crosslink_committees.add(crosslink_committee.getCommittee());
      }
    }
    List<Integer> crosslink_committee = crosslink_committees.get(0);

    for (int i = 0; i < crosslink_committee.size(); i++) {
      assert get_bitfield_bit(attestation.getAggregation_bitfield(), i) != 0b0
          || get_bitfield_bit(attestation.getCustody_bitfield(), i) == 0b0;
    }

    ArrayList<Integer> participants = get_attestation_participants(state, attestation.getData(),
        attestation.getAggregation_bitfield().toArray());
    ArrayList<Integer> custody_bit_1_participants = get_attestation_participants(state, attestation.getData(),
        attestation.getCustody_bitfield().toArray());
    ArrayList<Integer> custody_bit_0_participants = new ArrayList<>();
    for (Integer participant : participants) {
      if (custody_bit_1_participants.indexOf(participant) != -1) {
        custody_bit_0_participants.add(participant);
      }
    }

    ArrayList<Bytes48> pubkey0 = new ArrayList<>();
    for (int i = 0; i < custody_bit_0_participants.size(); i++) {
      pubkey0.add(state.getValidator_registry().get(i).getPubkey());
    }

    ArrayList<Bytes48> pubkey1 = new ArrayList<>();
    for (int i = 0; i < custody_bit_1_participants.size(); i++) {
      pubkey1.add(state.getValidator_registry().get(i).getPubkey());
    }

    assert bls_verify_multiple(
        bls_aggregate_pubkeys(pubkey0),
        bls_aggregate_pubkeys(pubkey1),
        hash_tree_root(new AttestationDataAndCustodyBit(attestation.getData(), false)),
        hash_tree_root(new AttestationDataAndCustodyBit(attestation.getData(), true)),
        attestation.getAggregate_signature(), get_domain(state.getFork(),
            slot_to_epoch(attestation.getData().getSlot()), DOMAIN_ATTESTATION));

    return true;
  }

  /**
   *
   * @param state
   * @param block
   */
  public static void deposits(BeaconState state, BeaconBlock block) {
    assert block.getBody().getDeposits().size() <= MAX_DEPOSITS;

    for (Deposit deposit : block.getBody().getDeposits()) {
      Bytes serialized_deposit_data = deposit.getDeposit_data().toBytes();

      assert verify_merkle_branch(Hash.keccak256(serialized_deposit_data),
          deposit.getBranch(), DEPOSIT_CONTRACT_TREE_DEPTH, deposit.getIndex().intValue(),
          state.getLatest_eth1_data().getDeposit_root());

      process_deposit(state, deposit.getDeposit_data().getDeposit_input().getPubkey(),
          deposit.getDeposit_data().getAmount(), deposit.getDeposit_data().getDeposit_input().getProof_of_possession(),
          deposit.getDeposit_data().getDeposit_input().getWithdrawal_credentials());
    }
  }

  /**
   *
   * @param state
   * @param block
   */
  public static void exits(BeaconState state, BeaconBlock block) {
    assert block.getBody().getExits().size() <= Constants.MAX_EXITS;
    for (Exit exit : block.getBody().getExits()) {
      Validator validator = state.getValidator_registry().get(exit.getValidator_index().intValue());
      assert validator.getExit_epoch().compareTo(get_entry_exit_effect_epoch(get_current_epoch(state))) > 0;
      assert get_current_epoch(state).compareTo(exit.getEpoch()) >= 0;

      Bytes32 exit_message = hash_tree_root(new Exit(exit.getEpoch(), exit.getValidator_index(), EMPTY_SIGNATURE));
      assert bls_verify(validator.getPubkey(), exit_message, exit.getSignature(),
          get_domain(state.getFork(), exit.getEpoch(), DOMAIN_EXIT));

      initiate_validator_exit(state, exit.getValidator_index().intValue());
    }
  }


  /**
   * Verify that the given ``leaf`` is on the merkle branch ``branch``.
   * @param leaf
   * @param branch
   * @param depth
   * @param index
   * @param root
   * @return
   */
  private static boolean verify_merkle_branch(Bytes32 leaf, List<Bytes32> branch, int depth, int index, Bytes32 root) {
    Bytes32 value = leaf;
    for (int i = 0; i < depth; i ++) {
      if (index / Math.pow(2, i) % 2 == 0) {
        value = Hash.keccak256(Bytes.concatenate(branch.get(i), value));
      } else {
        value = Hash.keccak256(Bytes.concatenate(value, branch.get(i)));
      }
    }
    return value == root;
  }

}
