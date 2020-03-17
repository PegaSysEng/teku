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

package tech.pegasys.artemis.beaconrestapi.handlers.validator;

import static com.google.common.primitives.UnsignedLong.ONE;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static tech.pegasys.artemis.beaconrestapi.RestApiConstants.RANDAO_REVEAL;

import io.javalin.http.Context;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.api.ValidatorDataProvider;
import tech.pegasys.artemis.api.schema.BLSSignature;
import tech.pegasys.artemis.beaconrestapi.RestApiConstants;
import tech.pegasys.artemis.beaconrestapi.schema.BadRequest;
import tech.pegasys.artemis.provider.JsonProvider;

public class GetBlockTest {

  private final tech.pegasys.artemis.util.bls.BLSSignature signatureInternal =
      tech.pegasys.artemis.util.bls.BLSSignature.random(1234);
  private BLSSignature signature = new BLSSignature(signatureInternal);
  private Context context = mock(Context.class);
  private final ValidatorDataProvider provider = mock(ValidatorDataProvider.class);
  private final JsonProvider jsonProvider = new JsonProvider();
  private GetBlock handler;

  @BeforeEach
  public void setup() {
    handler = new GetBlock(provider, jsonProvider);
  }

  @Test
  void shouldGetNoContentIfBlockCreationReturnsEmptyObject() throws Exception {
    final Map<String, List<String>> params =
        Map.of(
            RestApiConstants.SLOT, List.of("1"), RANDAO_REVEAL, List.of(signature.toHexString()));
    when(context.queryParamMap()).thenReturn(params);
    when(provider.getUnsignedBeaconBlockAtSlot(ONE, signature)).thenReturn(Optional.empty());
    handler.handle(context);

    verify(context).status(SC_NO_CONTENT);
  }

  @Test
  void shouldRequireThatRandaoRevealIsSet() throws Exception {
    badRequestParamsTest(Map.of(), "'randao_reveal' cannot be null or empty.");
  }

  @Test
  void shouldRequireThatSlotIsSet() throws Exception {
    badRequestParamsTest(
        Map.of(RANDAO_REVEAL, List.of(signature.toHexString())), "'slot' cannot be null or empty.");
  }

  private void badRequestParamsTest(final Map<String, List<String>> params, String message)
      throws Exception {
    when(context.queryParamMap()).thenReturn(params);

    handler.handle(context);
    verify(context).status(SC_BAD_REQUEST);

    if (StringUtils.isNotEmpty(message)) {
      BadRequest badRequest = new BadRequest(message);
      verify(context).result(jsonProvider.objectToJSON(badRequest));
    }
  }
}
