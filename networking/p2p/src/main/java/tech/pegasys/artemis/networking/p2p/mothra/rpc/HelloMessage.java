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

package tech.pegasys.artemis.networking.p2p.mothra.rpc;

import com.google.common.primitives.UnsignedLong;
import java.util.ArrayList;
import java.util.List;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.ssz.SSZ;
import tech.pegasys.artemis.util.SSZTypes.Bytes4;
import tech.pegasys.artemis.util.sos.SimpleOffsetSerializable;

public final class HelloMessage implements SimpleOffsetSerializable {

  private final Bytes4 forkVersion;
  private final Bytes32 finalizedRoot;
  private final UnsignedLong finalizedEpoch;
  private final Bytes32 headRoot;
  private final UnsignedLong headSlot;

  public HelloMessage(
      Bytes4 forkVersion,
      Bytes32 finalizedRoot,
      UnsignedLong finalizedEpoch,
      Bytes32 headRoot,
      UnsignedLong headSlot) {
    this.forkVersion = forkVersion;
    this.finalizedRoot = finalizedRoot;
    this.finalizedEpoch = finalizedEpoch;
    this.headRoot = headRoot;
    this.headSlot = headSlot;
  }

  /** * SSZ Interface Overrides ** */
  @Override
  public int getSSZFieldCount() {
    return 5;
  }

  @Override
  public List<Bytes> get_fixed_parts() {
    List<Bytes> fixedPartsList =
        new ArrayList<>(
            List.of(
                SSZ.encode(writer -> writer.writeFixedBytes(forkVersion.getWrappedBytes())),
                SSZ.encode(writer -> writer.writeFixedBytes(finalizedRoot)),
                SSZ.encodeUInt64(finalizedEpoch.longValue()),
                SSZ.encode(writer -> writer.writeFixedBytes(headRoot)),
                SSZ.encodeUInt64(headSlot.longValue())));
    return fixedPartsList;
  }
  /** * SSZ Interface Overrides ** */
  public Bytes4 forkVersion() {
    return forkVersion;
  }

  public Bytes32 finalizedRoot() {
    return finalizedRoot;
  }

  public UnsignedLong finalizedEpoch() {
    return finalizedEpoch;
  }

  public Bytes32 headRoot() {
    return headRoot;
  }

  public UnsignedLong headSlot() {
    return headSlot;
  }
}
