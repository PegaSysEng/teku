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

import io.javalin.core.util.Header;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import tech.pegasys.teku.api.ChainDataProvider;
import tech.pegasys.teku.api.DataProvider;
import tech.pegasys.teku.api.response.v1.beacon.GetStateRootResponse;
import tech.pegasys.teku.api.response.v1.beacon.GetStateValidatorBalancesResponse;
import tech.pegasys.teku.beaconrestapi.handlers.AbstractHandler;
import tech.pegasys.teku.beaconrestapi.schema.BadRequest;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.provider.JsonProvider;
import tech.pegasys.teku.storage.client.ChainDataUnavailableException;

import java.util.List;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static tech.pegasys.teku.beaconrestapi.CacheControlUtils.getMaxAgeForSlot;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.COMMITTEE_INDEX;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.COMMITTEE_INDEX_QUERY_DESCRIPTION;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.PARAM_EPOCH;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.PARAM_EPOCH_DESCRIPTION;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.PARAM_STATE_ID;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.PARAM_STATE_ID_DESCRIPTION;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.RES_BAD_REQUEST;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.RES_INTERNAL_ERROR;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.RES_NOT_FOUND;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.RES_OK;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.RES_SERVICE_UNAVAILABLE;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.SERVICE_UNAVAILABLE;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.SLOT;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.SLOT_QUERY_DESCRIPTION;
import static tech.pegasys.teku.beaconrestapi.RestApiConstants.TAG_V1_BEACON;

public class GetStateEpochCommittees extends AbstractHandler implements Handler {
  private static final Logger LOG = LogManager.getLogger();
  public static final String ROUTE = "/eth/v1/beacon/states/:state_id/committees/:epoch";

  private final ChainDataProvider provider;
  private final StateValidatorsUtil stateValidatorsUtil = new StateValidatorsUtil();

  public GetStateEpochCommittees(final DataProvider dataProvider, final JsonProvider jsonProvider) {
    super(jsonProvider);
    this.provider = dataProvider.getChainDataProvider();
  }

  GetStateEpochCommittees(final ChainDataProvider chainDataProvider, final JsonProvider jsonProvider) {
    super(jsonProvider);
    this.provider = chainDataProvider;
  }

  @OpenApi(
          path = ROUTE,
          method = HttpMethod.GET,
          summary = "Get all committees for epoch",
          tags = {TAG_V1_BEACON},
          description = "Retrieves the committees for the given state at the given epoch.",
          pathParams = {
                  @OpenApiParam(name = PARAM_STATE_ID, description = PARAM_STATE_ID_DESCRIPTION),
                  @OpenApiParam(name = PARAM_EPOCH, description = PARAM_EPOCH_DESCRIPTION)
          },
          queryParams = {
                  @OpenApiParam(
                          name = COMMITTEE_INDEX,
                          description = COMMITTEE_INDEX_QUERY_DESCRIPTION,
                          isRepeatable = true),
                  @OpenApiParam(
                          name = SLOT,
                          description = SLOT_QUERY_DESCRIPTION,
                          isRepeatable = true)
          },
          responses = {
                  @OpenApiResponse(
                          status = RES_OK,
                          content = @OpenApiContent(from = GetStateRootResponse.class)),
                  @OpenApiResponse(status = RES_BAD_REQUEST),
                  @OpenApiResponse(status = RES_NOT_FOUND),
                  @OpenApiResponse(status = RES_INTERNAL_ERROR),
                  @OpenApiResponse(status = RES_SERVICE_UNAVAILABLE, description = SERVICE_UNAVAILABLE)
          })
  @Override
  public void handle(@NotNull final Context ctx) throws Exception {
    try {
      final UInt64 slot = stateValidatorsUtil.parseSlotParam(provider, ctx);

      final List<Integer> validatorIndices = stateValidatorsUtil.parseValidatorsParam(provider, ctx);

      SafeFuture<Optional<GetStateValidatorBalancesResponse>> future =
              provider
                      .getValidatorsBalances(slot, validatorIndices)
                      .thenApply(result -> result.map(GetStateValidatorBalancesResponse::new));

      ctx.header(Header.CACHE_CONTROL, getMaxAgeForSlot(provider, slot));
      if (provider.isFinalized(slot)) {
        handlePossiblyGoneResult(ctx, future);
      } else {
        handlePossiblyMissingResult(ctx, future);
      }
    } catch (ChainDataUnavailableException ex) {
      LOG.trace(ex);
      ctx.status(SC_SERVICE_UNAVAILABLE);
      ctx.result(BadRequest.serviceUnavailable(jsonProvider));
    } catch (IllegalArgumentException ex) {
      LOG.trace(ex);
      ctx.status(SC_BAD_REQUEST);
      ctx.result(BadRequest.badRequest(jsonProvider, ex.getMessage()));
    }
  }
}
