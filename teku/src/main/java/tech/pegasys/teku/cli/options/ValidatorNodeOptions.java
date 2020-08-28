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

package tech.pegasys.teku.cli.options;

import picocli.CommandLine.Option;

public class ValidatorNodeOptions {

  @Option(
      names = {"--Xvalidator-node-only"},
      paramLabel = "<BOOLEAN>",
      description = "Run this node as a Validator Client only",
      fallbackValue = "true",
      arity = "0..1",
      hidden = true)
  private boolean validatorNodeOnly = false;

  @Option(
      names = {"--Xbeacon-node-api-endpoint"},
      paramLabel = "<ENDPOINT>",
      description = "Endpoint of the Beacon Node API",
      arity = "1",
      hidden = true)
  private String beaconNodeApiEndpoint = "http://127.0.0.1:5051";

  @Option(
      names = {"--Xbeacon-node-events-endpoint"},
      paramLabel = "<NETWORK>",
      description = "Endpoint of the Beacon Node Events WebSocket API",
      arity = "1",
      hidden = true)
  private String beaconNodeEventsWsEndpoint = "ws://127.0.0.1:9999";

  public boolean isValidatorNodeOnly() {
    return validatorNodeOnly;
  }

  public String getBeaconNodeApiEndpoint() {
    return beaconNodeApiEndpoint;
  }

  public String getBeaconNodeEventsWsEndpoint() {
    return beaconNodeEventsWsEndpoint;
  }
}
