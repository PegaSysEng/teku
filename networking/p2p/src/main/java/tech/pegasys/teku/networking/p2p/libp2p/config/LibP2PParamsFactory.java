/*
 * Copyright 2021 ConsenSys AG.
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

package tech.pegasys.teku.networking.p2p.libp2p.config;

import io.libp2p.core.PeerId;
import io.libp2p.pubsub.gossip.GossipParams;
import io.libp2p.pubsub.gossip.GossipPeerScoreParams;
import io.libp2p.pubsub.gossip.GossipScoreParams;
import io.libp2p.pubsub.gossip.GossipTopicScoreParams;
import io.libp2p.pubsub.gossip.GossipTopicsScoreParams;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kotlin.jvm.functions.Function1;
import tech.pegasys.teku.networking.p2p.gossip.config.GossipConfig;
import tech.pegasys.teku.networking.p2p.gossip.config.GossipPeerScoringConfig;
import tech.pegasys.teku.networking.p2p.gossip.config.GossipScoringConfig;
import tech.pegasys.teku.networking.p2p.gossip.config.GossipTopicScoringConfig;
import tech.pegasys.teku.networking.p2p.libp2p.LibP2PNodeId;
import tech.pegasys.teku.networking.p2p.peer.NodeId;

public class LibP2PParamsFactory {
  public static GossipParams createGossipParams(final GossipConfig gossipConfig) {
    return GossipParams.builder()
        .D(gossipConfig.getD())
        .DLow(gossipConfig.getDLow())
        .DHigh(gossipConfig.getDHigh())
        .DLazy(gossipConfig.getDLazy())
        .fanoutTTL(gossipConfig.getFanoutTTL())
        .gossipSize(gossipConfig.getAdvertise())
        .gossipHistoryLength(gossipConfig.getHistory())
        .heartbeatInterval(gossipConfig.getHeartbeatInterval())
        .floodPublish(true)
        .seenTTL(gossipConfig.getSeenTTL())
        .maxPublishedMessages(1000)
        .maxTopicsPerPublishedMessage(1)
        .maxSubscriptions(200)
        .maxGraftMessages(200)
        .maxPruneMessages(200)
        .maxPeersPerPruneMessage(1000)
        .maxIHaveLength(5000)
        .maxIWantMessageIds(5000)
        .build();
  }

  public static GossipScoreParams createGossipScoreParams(final GossipScoringConfig config) {
    return GossipScoreParams.builder()
        .peerScoreParams(createPeerScoreParams(config.getPeerScoringConfig()))
        .topicsScoreParams(createTopicsScoreParams(config))
        .gossipThreshold(config.getGossipThreshold())
        .publishThreshold(config.getPublishThreshold())
        .graylistThreshold(config.getGraylistThreshold())
        .acceptPXThreshold(config.getAcceptPXThreshold())
        .opportunisticGraftThreshold(config.getOpportunisticGraftThreshold())
        .build();
  }

  public static GossipPeerScoreParams createPeerScoreParams(final GossipPeerScoringConfig config) {
    // TODO - move lambda parameters into config
    // Direct peers lambda
    final List<NodeId> directPeers = config.getDirectPeers();
    final Function1<? super PeerId, Boolean> isDirectPeer =
        peerId -> {
          if (directPeers.isEmpty()) {
            return false;
          }
          final byte[] peerIdBytes = peerId.getBytes();
          return directPeers.stream()
              .anyMatch(dp -> Arrays.equals(peerIdBytes, dp.toBytes().toArrayUnsafe()));
        };

    // App-specific score
    final Function1<? super PeerId, Double> appSpecificScore =
        peerId -> config.getAppSpecificScore().scorePeer(new LibP2PNodeId(peerId));

    // Ip whitelisting
    final List<InetAddress> whitelistedIps = config.getWhitelistedIps();
    final Function1<? super String, Boolean> isIpWhitelisted =
        ipString -> {
          try {
            return !whitelistedIps.isEmpty()
                && whitelistedIps.contains(InetAddress.getByName(ipString));
          } catch (UnknownHostException e) {
            return false;
          }
        };

    return GossipPeerScoreParams.builder()
        .topicScoreCap(config.getTopicScoreCap())
        .isDirect(isDirectPeer)
        .appSpecificScore(appSpecificScore)
        .appSpecificWeight(config.getAppSpecificWeight())
        .ipWhitelisted(isIpWhitelisted)
        .ipColocationFactorWeight(config.getIpColocationFactorWeight())
        .ipColocationFactorThreshold(config.getIpColocationFactorThreshold())
        .behaviourPenaltyWeight(config.getBehaviourPenaltyWeight())
        .behaviourPenaltyDecay(config.getBehaviourPenaltyDecay())
        .behaviourPenaltyThreshold(config.getBehaviourPenaltyThreshold())
        .decayInterval(config.getDecayInterval())
        .decayToZero(config.getDecayToZero())
        .retainScore(config.getRetainScore())
        .build();
  }

  public static GossipTopicsScoreParams createTopicsScoreParams(final GossipScoringConfig config) {
    final GossipTopicScoreParams defaultTopicParams =
        createTopicScoreParams(config.getDefaultTopicScoringConfig());
    final Map<String, GossipTopicScoreParams> topicParams =
        config.getTopicScoringConfig().entrySet().stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, e -> createTopicScoreParams(e.getValue())));
    return new GossipTopicsScoreParams(defaultTopicParams, topicParams);
  }

  public static GossipTopicScoreParams createTopicScoreParams(
      final GossipTopicScoringConfig config) {
    return GossipTopicScoreParams.builder()
        .topicWeight(config.getTopicWeight())
        .timeInMeshWeight(config.getTimeInMeshWeight())
        .timeInMeshQuantum(config.getTimeInMeshQuantum())
        .timeInMeshCap(config.getTimeInMeshCap())
        .firstMessageDeliveriesWeight(config.getFirstMessageDeliveriesWeight())
        .firstMessageDeliveriesDecay(config.getFirstMessageDeliveriesDecay())
        .firstMessageDeliveriesCap(config.getFirstMessageDeliveriesCap())
        .meshMessageDeliveriesWeight(config.getMeshMessageDeliveriesWeight())
        .meshMessageDeliveriesDecay(config.getMeshMessageDeliveriesDecay())
        .meshMessageDeliveriesThreshold(config.getMeshMessageDeliveriesThreshold())
        .meshMessageDeliveriesCap(config.getMeshMessageDeliveriesCap())
        .meshMessageDeliveriesActivation(config.getMeshMessageDeliveriesActivation())
        .meshMessageDeliveryWindow(config.getMeshMessageDeliveryWindow())
        .meshFailurePenaltyWeight(config.getMeshFailurePenaltyWeight())
        .meshFailurePenaltyDecay(config.getMeshFailurePenaltyDecay())
        .invalidMessageDeliveriesWeight(config.getInvalidMessageDeliveriesWeight())
        .invalidMessageDeliveriesDecay(config.getInvalidMessageDeliveriesDecay())
        .build();
  }
}
