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

package tech.pegasys.artemis.service.serviceutils;

import tech.pegasys.artemis.util.async.GoodFuture;

public class NoopService extends Service {

  @Override
  protected GoodFuture<?> doStart() {
    return GoodFuture.completedFuture(null);
  }

  @Override
  protected GoodFuture<?> doStop() {
    return GoodFuture.completedFuture(null);
  }
}
