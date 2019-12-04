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

package tech.pegasys.artemis.networking.p2p.api;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import tech.pegasys.artemis.networking.p2p.peer.NodeId;
import tech.pegasys.artemis.networking.p2p.peer.Peer;

public interface P2PNetwork {
  /**
   * Connects to a Peer.
   *
   * @param peer Peer to connect to.
   * @return Future of the established PeerConnection
   */
  CompletableFuture<?> connect(String peer);

  Optional<? extends Peer> getPeer(NodeId id);

  Stream<? extends Peer> streamPeers();

  default long getPeerCount() {
    return streamPeers().count();
  }

  String getNodeAddress();

  NodeId getNodeId();

  /**
   * starts the p2p network layer
   *
   * @return
   */
  CompletableFuture<?> start();

  /** Stops the P2P network layer. */
  void stop();
}
