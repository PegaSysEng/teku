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

package tech.pegasys.artemis.beaconrestapi.beaconhandlers;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static tech.pegasys.artemis.beaconrestapi.RestApiConstants.EPOCH;
import static tech.pegasys.artemis.beaconrestapi.RestApiConstants.RES_BAD_REQUEST;
import static tech.pegasys.artemis.beaconrestapi.RestApiConstants.RES_NOT_FOUND;
import static tech.pegasys.artemis.beaconrestapi.RestApiConstants.RES_OK;
import static tech.pegasys.artemis.beaconrestapi.RestApiConstants.ROOT;
import static tech.pegasys.artemis.beaconrestapi.RestApiConstants.SLOT;
import static tech.pegasys.artemis.beaconrestapi.RestApiUtils.validateQueryParameter;
import static tech.pegasys.artemis.datastructures.util.BeaconStateUtil.compute_start_slot_at_epoch;

import com.google.common.primitives.UnsignedLong;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import java.util.List;
import java.util.Map;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.artemis.beaconrestapi.schema.BadRequest;
import tech.pegasys.artemis.beaconrestapi.schema.BeaconBlockResponse;
import tech.pegasys.artemis.provider.JsonProvider;
import tech.pegasys.artemis.storage.CombinedChainDataClient;

public class BeaconBlockHandler implements Handler {

  public static final String ROUTE = "/beacon/block";
  private final JsonProvider jsonProvider;
  private final CombinedChainDataClient combinedChainDataClient;

  public BeaconBlockHandler(
      final CombinedChainDataClient combinedChainDataClient, final JsonProvider jsonProvider) {
    this.jsonProvider = jsonProvider;
    this.combinedChainDataClient = combinedChainDataClient;
  }

  @OpenApi(
      path = ROUTE,
      method = HttpMethod.GET,
      summary = "Get the beacon chain block that matches the criteria.",
      tags = {"Beacon"},
      queryParams = {
        @OpenApiParam(name = EPOCH, description = "Epoch number to query."),
        @OpenApiParam(name = SLOT, description = "Slot to query in the canonical chain."),
        @OpenApiParam(name = ROOT, description = "Tree hash root to query.")
      },
      description =
          "Returns the beacon chain block that matches the specified epoch, slot, or tree hash root.",
      responses = {
        @OpenApiResponse(
            status = RES_OK,
            content = @OpenApiContent(from = BeaconBlockResponse.class)),
        @OpenApiResponse(status = RES_BAD_REQUEST, description = "Invalid parameter supplied"),
        @OpenApiResponse(status = RES_NOT_FOUND, description = "Specified block not found")
      })
  @Override
  public void handle(final Context ctx) throws Exception {
    try {
      if (ctx.queryParamMap().size() > 1) {
        throw new IllegalArgumentException(
            "Too many query parameters specified. Please supply only one.");
      }

      final Map<String, List<String>> queryParamMap = ctx.queryParamMap();
      if (ctx.queryParamMap().containsKey(ROOT)) {
        final Bytes32 blockParam =
            Bytes32.fromHexString(validateQueryParameter(queryParamMap, ROOT));

        ctx.result(
            combinedChainDataClient
                .getBlockByBlockRoot(blockParam)
                .thenApplyChecked(
                    block -> {
                      if (block.isPresent()) {
                        return jsonProvider.objectToJSON(new BeaconBlockResponse(block.get()));
                      } else {
                        ctx.status(SC_NOT_FOUND);
                        return null;
                      }
                    }));
        return;
      }

      final UnsignedLong slot;
      if (ctx.queryParamMap().containsKey(EPOCH)) {
        slot =
            compute_start_slot_at_epoch(
                UnsignedLong.valueOf(validateQueryParameter(queryParamMap, EPOCH)));
      } else if (ctx.queryParamMap().containsKey(SLOT)) {
        slot = UnsignedLong.valueOf(validateQueryParameter(queryParamMap, SLOT));
      } else {
        throw new IllegalArgumentException(
            "Query parameter missing. Must specify one of root or epoch or slot.");
      }

      ctx.result(
          combinedChainDataClient
              .getBlockBySlot(slot)
              .thenApplyChecked(
                  block -> {
                    if (block.isPresent()) {
                      return jsonProvider.objectToJSON(new BeaconBlockResponse(block.get()));
                    } else {
                      ctx.status(SC_NOT_FOUND);
                      return null;
                    }
                  }));

    } catch (final IllegalArgumentException e) {
      ctx.result(jsonProvider.objectToJSON(new BadRequest(e.getMessage())));
    }
  }
}
