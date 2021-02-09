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

package tech.pegasys.teku.spec;

import static com.google.common.base.Preconditions.checkNotNull;

import tech.pegasys.teku.spec.constants.SpecConstants;
import tech.pegasys.teku.spec.schemas.SchemaDefinitions;
import tech.pegasys.teku.spec.schemas.genesis.SchemaDefinitionsGenesis;
import tech.pegasys.teku.spec.util.CommitteeUtil;
import tech.pegasys.teku.spec.util.genesis.CommitteeUtilGenesis;

public class Spec {
  private final SpecConstants constants;
  private final CommitteeUtil committeeUtil;
  private final SchemaDefinitions schemaDefinitions;

  private Spec(
      final SpecConstants constants,
      final CommitteeUtil committeeUtil,
      final SchemaDefinitions schemaDefinitions) {
    this.constants = constants;
    this.committeeUtil = committeeUtil;
    this.schemaDefinitions = schemaDefinitions;
  }

  public static Builder builder() {
    return new Builder();
  }

  public SpecConstants getConstants() {
    return constants;
  }

  public CommitteeUtil getCommitteeUtil() {
    return committeeUtil;
  }

  public SchemaDefinitions getSchemaDefinitions() {
    return schemaDefinitions;
  }

  public static class Builder {
    private SpecConstants constants;
    private CommitteeUtil committeeUtil;
    private SchemaDefinitions schemaDefinitions;

    private Builder() {}

    public Spec build() {
      validate();
      return new Spec(constants, committeeUtil, schemaDefinitions);
    }

    public Spec buildGenesis(final SpecConfiguration config) {
      return Spec.builder()
          .constants(config.constants())
          .committeeUtil(new CommitteeUtilGenesis(config.constants()))
          .schemaDefinitions(new SchemaDefinitionsGenesis(config.constants()))
          .build();
    };

    private void validate() {
      // TODO
    }

    public Builder constants(final SpecConstants constants) {
      checkNotNull(constants);
      this.constants = constants;
      return this;
    }

    public Builder committeeUtil(final CommitteeUtil committeeUtil) {
      checkNotNull(committeeUtil);
      this.committeeUtil = committeeUtil;
      return this;
    }

    public Builder schemaDefinitions(final SchemaDefinitions schemaDefinitions) {
      checkNotNull(schemaDefinitions);
      this.schemaDefinitions = schemaDefinitions;
      return this;
    }
  }
}