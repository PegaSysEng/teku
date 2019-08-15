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

package tech.pegasys.artemis.validator.coordinator;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import tech.pegasys.artemis.util.bls.BLSKeyPair;

public class FixedValidatorKeyPairFactory implements ValidatorKeyPairFactory {

  @Override
  public List<BLSKeyPair> generateKeyPairs(int startIndex, int endIndex) {
    return IntStream.rangeClosed(startIndex, endIndex)
        .mapToObj(BLSKeyPair::random)
        .collect(Collectors.toList());
  }
}
