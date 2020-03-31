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

package tech.pegasys.artemis.validator.client;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.pegasys.artemis.datastructures.state.Fork;
import tech.pegasys.artemis.util.async.AsyncRunner;
import tech.pegasys.artemis.util.async.SafeFuture;
import tech.pegasys.artemis.validator.api.ValidatorApiChannel;

public class ForkProvider {
  private static final Logger LOG = LogManager.getLogger();

  private AsyncRunner asyncRunner;
  private final ValidatorApiChannel validatorApiChannel;
  private final int retryDelayInSeconds;
  private final int refreshDelayInSeconds;

  private volatile Optional<Fork> currentFork = Optional.empty();

  public ForkProvider(
      final AsyncRunner asyncRunner,
      final ValidatorApiChannel validatorApiChannel,
      final int retryDelayInSeconds,
      final int refreshDelayInSeconds) {
    this.asyncRunner = asyncRunner;
    this.validatorApiChannel = validatorApiChannel;
    this.retryDelayInSeconds = retryDelayInSeconds;
    this.refreshDelayInSeconds = refreshDelayInSeconds;
  }

  public SafeFuture<Fork> getFork() {
    return currentFork.map(SafeFuture::completedFuture).orElseGet(this::loadFork);
  }

  public SafeFuture<Fork> loadFork() {
    return requestFork()
        .exceptionallyCompose(
            error -> {
              LOG.error("Failed to retrieve current fork. Retrying after delay", error);
              return asyncRunner.runAfterDelay(
                  this::getFork, retryDelayInSeconds, TimeUnit.SECONDS);
            });
  }

  public SafeFuture<Fork> requestFork() {
    return validatorApiChannel
        .getFork()
        .thenCompose(
            maybeFork -> {
              if (maybeFork.isEmpty()) {
                LOG.trace("Fork not available, retrying");
                return asyncRunner.runAfterDelay(
                    this::requestFork, retryDelayInSeconds, TimeUnit.SECONDS);
              }
              final Fork fork = maybeFork.orElseThrow();
              currentFork = Optional.of(fork);
              // Periodically refresh the current fork.
              asyncRunner
                  .runAfterDelay(this::loadFork, refreshDelayInSeconds, TimeUnit.SECONDS)
                  .reportExceptions();
              return SafeFuture.completedFuture(fork);
            });
  }
}
