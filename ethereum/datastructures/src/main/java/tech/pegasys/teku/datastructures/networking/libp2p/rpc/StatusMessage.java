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

package tech.pegasys.teku.datastructures.networking.libp2p.rpc;

import static tech.pegasys.teku.datastructures.util.BeaconStateUtil.compute_fork_digest;

import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.ssz.SSZTypes.Bytes4;
import tech.pegasys.teku.ssz.backing.containers.Container5;
import tech.pegasys.teku.ssz.backing.containers.ContainerType5;
import tech.pegasys.teku.ssz.backing.tree.TreeNode;
import tech.pegasys.teku.ssz.backing.type.SszPrimitiveSchemas;
import tech.pegasys.teku.ssz.backing.view.SszPrimitives.SszBytes32;
import tech.pegasys.teku.ssz.backing.view.SszPrimitives.SszBytes4;
import tech.pegasys.teku.ssz.backing.view.SszPrimitives.SszUInt64;
import tech.pegasys.teku.util.config.Constants;

public class StatusMessage
    extends Container5<StatusMessage, SszBytes4, SszBytes32, SszUInt64, SszBytes32, SszUInt64>
    implements RpcRequest {

  public static class StatusMessageType
      extends ContainerType5<
          StatusMessage, SszBytes4, SszBytes32, SszUInt64, SszBytes32, SszUInt64> {

    public StatusMessageType() {
      super(
          "StatusMessage",
          namedSchema("forkDigest", SszPrimitiveSchemas.BYTES4_SCHEMA),
          namedSchema("finalizedRoot", SszPrimitiveSchemas.BYTES32_SCHEMA),
          namedSchema("finalizedEpoch", SszPrimitiveSchemas.UINT64_SCHEMA),
          namedSchema("headRoot", SszPrimitiveSchemas.BYTES32_SCHEMA),
          namedSchema("headSlot", SszPrimitiveSchemas.UINT64_SCHEMA));
    }

    @Override
    public StatusMessage createFromBackingNode(TreeNode node) {
      return new StatusMessage(this, node);
    }
  }

  public static final StatusMessageType TYPE = new StatusMessageType();

  private StatusMessage(StatusMessageType type, TreeNode backingNode) {
    super(type, backingNode);
  }

  public StatusMessage(
      Bytes4 forkDigest,
      Bytes32 finalizedRoot,
      UInt64 finalizedEpoch,
      Bytes32 headRoot,
      UInt64 headSlot) {
    super(
        TYPE,
        new SszBytes4(forkDigest),
        new SszBytes32(finalizedRoot),
        new SszUInt64(finalizedEpoch),
        new SszBytes32(headRoot),
        new SszUInt64(headSlot));
  }

  public static StatusMessage createPreGenesisStatus() {
    return new StatusMessage(
        createPreGenesisForkDigest(), Bytes32.ZERO, UInt64.ZERO, Bytes32.ZERO, UInt64.ZERO);
  }

  private static Bytes4 createPreGenesisForkDigest() {
    final Bytes4 genesisFork = Constants.GENESIS_FORK_VERSION;
    final Bytes32 emptyValidatorsRoot = Bytes32.ZERO;
    return compute_fork_digest(genesisFork, emptyValidatorsRoot);
  }

  public Bytes4 getForkDigest() {
    return getField0().get();
  }

  public Bytes32 getFinalizedRoot() {
    return getField1().get();
  }

  public UInt64 getFinalizedEpoch() {
    return getField2().get();
  }

  public Bytes32 getHeadRoot() {
    return getField3().get();
  }

  public UInt64 getHeadSlot() {
    return getField4().get();
  }

  @Override
  public int getMaximumRequestChunks() {
    return 1;
  }
}
