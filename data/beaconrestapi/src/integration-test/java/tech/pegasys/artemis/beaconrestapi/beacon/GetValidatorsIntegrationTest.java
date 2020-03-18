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

package tech.pegasys.artemis.beaconrestapi.beacon;

import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.beaconrestapi.AbstractBeaconRestAPIIntegrationTest;
import tech.pegasys.artemis.beaconrestapi.RestApiConstants;
import tech.pegasys.artemis.beaconrestapi.handlers.beacon.GetValidators;

public class GetValidatorsIntegrationTest extends AbstractBeaconRestAPIIntegrationTest {

  @Test
  public void shouldReturnNoContentIfStoreNotDefined_implicitlyQueryLatest() throws Exception {
    when(chainStorageClient.getStore()).thenReturn(null);

    final Response response = getLatest();
    assertThat(response.code()).isEqualTo(SC_NO_CONTENT);
    assertThat(response.body().string()).isEmpty();
  }

  @Test
  public void shouldReturnNoContentIfStoreNotDefined_queryByEpoch() throws Exception {
    when(chainStorageClient.getStore()).thenReturn(null);

    final Response response = getByEpoch(1);
    assertThat(response.code()).isEqualTo(SC_NO_CONTENT);
    assertThat(response.body().string()).isEmpty();
  }

  private Response getLatest() throws IOException {
    return getResponse(GetValidators.ROUTE);
  }

  private Response getByEpoch(final int epoch) throws IOException {
    return getResponse(
        GetValidators.ROUTE, Map.of(RestApiConstants.EPOCH, Integer.toString(epoch, 10)));
  }
}
