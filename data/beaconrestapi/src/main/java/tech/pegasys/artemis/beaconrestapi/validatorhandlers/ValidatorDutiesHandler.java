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

package tech.pegasys.artemis.beaconrestapi.validatorhandlers;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static tech.pegasys.artemis.beaconrestapi.CacheControlUtils.getMaxAgeForSlot;
import static tech.pegasys.artemis.beaconrestapi.RestApiConstants.EPOCH;
import static tech.pegasys.artemis.beaconrestapi.RestApiConstants.NO_CONTENT_PRE_GENESIS;
import static tech.pegasys.artemis.beaconrestapi.RestApiConstants.RES_INTERNAL_ERROR;
import static tech.pegasys.artemis.beaconrestapi.RestApiConstants.RES_NO_CONTENT;
import static tech.pegasys.artemis.beaconrestapi.RestApiConstants.RES_OK;
import static tech.pegasys.artemis.beaconrestapi.RestApiConstants.TAG_VALIDATOR;
import static tech.pegasys.artemis.beaconrestapi.SingleQueryParameterUtils.getParameterValueAsUnsignedLong;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.primitives.UnsignedLong;
import io.javalin.core.util.Header;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import java.util.List;
import java.util.Map;
import tech.pegasys.artemis.api.ChainDataProvider;
import tech.pegasys.artemis.api.schema.Committee;
import tech.pegasys.artemis.api.schema.ValidatorDuties;
import tech.pegasys.artemis.beaconrestapi.schema.BadRequest;
import tech.pegasys.artemis.datastructures.util.BeaconStateUtil;
import tech.pegasys.artemis.provider.JsonProvider;
import tech.pegasys.artemis.util.async.SafeFuture;

public class ValidatorDutiesHandler implements Handler {
  private final ChainDataProvider provider;

  public ValidatorDutiesHandler(final ChainDataProvider provider, final JsonProvider jsonProvider) {
    this.provider = provider;
    this.jsonProvider = jsonProvider;
  }

  public static final String ROUTE = "/validator/duties";
  private final JsonProvider jsonProvider;

  @OpenApi(
      path = ROUTE,
      method = HttpMethod.GET,
      summary = "Returns validator duties that match the specified query.",
      tags = {TAG_VALIDATOR},
      description = "Returns validator duties for the given epoch.",
      queryParams = {
        @OpenApiParam(
            name = EPOCH,
            description = "Epoch to query. If not specified, current epoch is used.")
      },
      responses = {
        @OpenApiResponse(status = RES_OK, content = @OpenApiContent(from = ValidatorDuties.class)),
        @OpenApiResponse(status = RES_NO_CONTENT, description = NO_CONTENT_PRE_GENESIS),
        @OpenApiResponse(status = RES_INTERNAL_ERROR)
      })
  @Override
  public void handle(Context ctx) throws Exception {
    final Map<String, List<String>> parameters = ctx.queryParamMap();
    try {
      if (!provider.isStoreAvailable()) {
        ctx.status(SC_NO_CONTENT);
        return;
      }
      UnsignedLong epoch = getParameterValueAsUnsignedLong(parameters, EPOCH);
      final SafeFuture<List<Committee>> future = provider.getCommitteesAtEpoch(epoch);

      ctx.result(future.thenApplyChecked(state -> handleResponseContext(ctx, state, epoch)));

    } catch (final IllegalArgumentException e) {
      ctx.result(jsonProvider.objectToJSON(new BadRequest(e.getMessage())));
      ctx.status(SC_BAD_REQUEST);
    }
  }

  private String handleResponseContext(Context ctx, List<Committee> committees, UnsignedLong epoch)
      throws JsonProcessingException {
    UnsignedLong slot = BeaconStateUtil.compute_start_slot_at_epoch(epoch);
    ctx.header(Header.CACHE_CONTROL, getMaxAgeForSlot(provider, slot));
    return jsonProvider.objectToJSON(committees);
  }
}
