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

package tech.pegasys.artemis.networking.eth2;

import com.google.common.primitives.UnsignedLong;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.ssz.SSZ;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.datastructures.networking.libp2p.rpc.StatusMessage;
import tech.pegasys.artemis.datastructures.state.Fork;
import tech.pegasys.artemis.networking.eth2.peers.Eth2Peer;
import tech.pegasys.artemis.networking.eth2.rpc.beaconchain.BeaconChainMethods;
import tech.pegasys.artemis.networking.eth2.rpc.core.ResponseStream;
import tech.pegasys.artemis.networking.eth2.rpc.core.RpcException;
import tech.pegasys.artemis.util.Waiter;
import tech.pegasys.artemis.util.async.GoodFuture;

public class ErrorConditionsIntegrationTest {

  private final Eth2NetworkFactory networkFactory = new Eth2NetworkFactory();

  @AfterEach
  public void tearDown() {
    networkFactory.stopAll();
  }

  @Test
  public void shouldRejectInvalidRequests() throws Exception {
    final Eth2Network network1 = networkFactory.builder().startNetwork();
    final Eth2Network network2 = networkFactory.builder().peer(network1).startNetwork();

    final Eth2Peer peer = network1.getPeer(network2.getNodeId()).orElseThrow();

    final GoodFuture<StatusMessage> response =
        peer.sendRequest(BeaconChainMethods.STATUS, new InvalidStatusMessage())
            .thenCompose(ResponseStream::expectSingleResponse);

    Assertions.assertThatThrownBy(() -> Waiter.waitFor(response))
        .isInstanceOf(ExecutionException.class)
        .extracting(Throwable::getCause)
        .isEqualToComparingFieldByField(RpcException.MALFORMED_REQUEST_ERROR);
  }

  // Deliberately doesn't serialize to a valid STATUS message.
  private static class InvalidStatusMessage extends StatusMessage {

    public InvalidStatusMessage() {
      super(Fork.VERSION_ZERO, Bytes32.ZERO, UnsignedLong.ZERO, Bytes32.ZERO, UnsignedLong.ZERO);
    }

    @Override
    public int getSSZFieldCount() {
      return 1;
    }

    @Override
    public List<Bytes> get_fixed_parts() {
      return List.of(SSZ.encode(writer -> writer.writeFixedBytes(Bytes.fromHexString("0xABCDEF"))));
    }
  }
}
