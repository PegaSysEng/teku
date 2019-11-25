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

package tech.pegasys.artemis.networking.p2p.jvmlibp2p.rpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.primitives.UnsignedLong;
import io.netty.buffer.Unpooled;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.datastructures.networking.libp2p.rpc.StatusMessage;
import tech.pegasys.artemis.networking.p2p.jvmlibp2p.rpc.encodings.SszEncoding;
import tech.pegasys.artemis.util.SSZTypes.Bytes4;

class RequestRpcCodecTest {

  private static final Bytes RECORDED_STATUS_REQUEST_BYTES =
      Bytes.fromHexString(
          "0x54000000000000000000000000000000000000000000000000000000000000000000000000000000000000000030A903798306695D21D1FAA76363A0070677130835E503760B0E84479B7819E60000000000000000");
  private static final StatusMessage RECORDED_STATUS_MESSAGE_DATA =
      new StatusMessage(
          new Bytes4(Bytes.of(0, 0, 0, 0)),
          Bytes32.ZERO,
          UnsignedLong.ZERO,
          Bytes32.fromHexString(
              "0x30A903798306695D21D1FAA76363A0070677130835E503760B0E84479B7819E6"),
          UnsignedLong.ZERO);

  private final SszEncoding encoding = new SszEncoding();

  @Test
  void testStatusRoundtripSerialization() throws Exception {
    final StatusMessage expected =
        new StatusMessage(
            Bytes4.rightPad(Bytes.of(4)),
            Bytes32.random(),
            UnsignedLong.ZERO,
            Bytes32.random(),
            UnsignedLong.ZERO);

    final Bytes encoded = new RpcEncoder(encoding).encodeRequest(expected);
    final AtomicReference<StatusMessage> decodedRequest = new AtomicReference<>();
    final RequestRpcCodec<StatusMessage> decoder =
        new RequestRpcCodec<>(decodedRequest::set, StatusMessage.class, encoding);
    decoder.onDataReceived(Unpooled.wrappedBuffer(encoded.toArrayUnsafe()));
    decoder.close();

    assertThat(decodedRequest.get()).isEqualTo(expected);
  }

  @Test
  public void shouldDecodeStatusMessageRequest() throws Exception {
    final AtomicReference<StatusMessage> decodedRequest = new AtomicReference<>();
    final RequestRpcCodec<StatusMessage> decoder =
        new RequestRpcCodec<>(decodedRequest::set, StatusMessage.class, encoding);
    decoder.onDataReceived(Unpooled.wrappedBuffer(RECORDED_STATUS_REQUEST_BYTES.toArrayUnsafe()));
    assertThat(decodedRequest.get())
        .usingRecursiveComparison()
        .isEqualTo(RECORDED_STATUS_MESSAGE_DATA);
  }
}
