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

package tech.pegasys.teku.beaconrestapi.handlers.v1.beacon;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.PARAM_STATE_ID;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.PARAM_STATE_ID_DESCRIPTION;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.RES_BAD_REQUEST;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.RES_INTERNAL_ERROR;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.RES_NOT_FOUND;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.RES_OK;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.RES_SERVICE_UNAVAILABLE;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.SERVICE_UNAVAILABLE;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.TAG_V1_BEACON;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import tech.pegasys.teku.api.ChainDataProvider;
import tech.pegasys.teku.api.DataProvider;
import tech.pegasys.teku.api.response.v1.beacon.FinalityCheckpointsResponse;
import tech.pegasys.teku.api.response.v1.beacon.GetStateFinalityCheckpointsResponse;
import tech.pegasys.teku.api.schema.BeaconState;
import tech.pegasys.teku.api.schema.Checkpoint;
import tech.pegasys.teku.beaconrestapi.handlers.AbstractHandler;
import tech.pegasys.teku.beaconrestapi.schema.BadRequest;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.provider.JsonProvider;
import tech.pegasys.teku.storage.client.ChainDataUnavailableException;

public class GetStateFinalityCheckpoints extends AbstractHandler implements Handler {
  private static final Logger LOG = LogManager.getLogger();
  public static final String ROUTE = "/eth/v1/beacon/states/:state_id/finality_checkpoints";
  private final ChainDataProvider chainDataProvider;

  public GetStateFinalityCheckpoints(
      final DataProvider dataProvider, final JsonProvider jsonProvider) {
    super(jsonProvider);
    this.chainDataProvider = dataProvider.getChainDataProvider();
  }

  GetStateFinalityCheckpoints(
      final ChainDataProvider chainDataProvider, final JsonProvider jsonProvider) {
    super(jsonProvider);
    this.chainDataProvider = chainDataProvider;
  }

  @OpenApi(
      path = ROUTE,
      method = HttpMethod.GET,
      summary = "Get state finality checkpoints",
      tags = {TAG_V1_BEACON},
      description =
          "Returns finality checkpoints for state with given 'stateId'. In case finality is not yet achieved, checkpoint should return epoch 0 and ZERO_HASH as root.",
      pathParams = {@OpenApiParam(name = PARAM_STATE_ID, description = PARAM_STATE_ID_DESCRIPTION)},
      responses = {
        @OpenApiResponse(
            status = RES_OK,
            content = @OpenApiContent(from = GetStateFinalityCheckpointsResponse.class)),
        @OpenApiResponse(status = RES_BAD_REQUEST),
        @OpenApiResponse(status = RES_NOT_FOUND),
        @OpenApiResponse(status = RES_INTERNAL_ERROR),
        @OpenApiResponse(status = RES_SERVICE_UNAVAILABLE, description = SERVICE_UNAVAILABLE)
      })
  @Override
  public void handle(@NotNull final Context ctx) throws Exception {
    final Map<String, String> pathParams = ctx.pathParamMap();
    try {
      final Optional<UInt64> maybeSlot =
          chainDataProvider.stateParameterToSlot(pathParams.get(PARAM_STATE_ID));
      if (maybeSlot.isEmpty()) {
        ctx.status(SC_NOT_FOUND);
        return;
      }
      SafeFuture<Optional<BeaconState>> future = chainDataProvider.getStateAtSlot(maybeSlot.get());
      handleOptionalResult(ctx, future, this::handleResult, SC_NOT_FOUND);
    } catch (ChainDataUnavailableException ex) {
      LOG.trace(ex);
      ctx.status(SC_SERVICE_UNAVAILABLE);
    } catch (IllegalArgumentException ex) {
      LOG.trace(ex);
      ctx.status(SC_BAD_REQUEST);
      ctx.result(BadRequest.badRequest(jsonProvider, ex.getMessage()));
    }
  }

  private Optional<String> handleResult(Context ctx, final BeaconState response)
      throws JsonProcessingException {
    boolean isFinalized = response.finalized_checkpoint.epoch.isGreaterThan(UInt64.ZERO);
    FinalityCheckpointsResponse finalityCheckpointsResponse =
        new FinalityCheckpointsResponse(
            isFinalized ? response.previous_justified_checkpoint : Checkpoint.EMPTY,
            isFinalized ? response.current_justified_checkpoint : Checkpoint.EMPTY,
            isFinalized ? response.finalized_checkpoint : Checkpoint.EMPTY);
    return Optional.of(
        jsonProvider.objectToJSON(
            new GetStateFinalityCheckpointsResponse(finalityCheckpointsResponse)));
  }
}
