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

package tech.pegasys.teku.api.schema;

import com.google.common.primitives.UnsignedLong;
import io.swagger.v3.oas.annotations.media.Schema;

public class SyncStatus {
  @Schema(type = "string", format = "uint64")
  public final UnsignedLong startingSlot;

  @Schema(type = "string", format = "uint64")
  public final UnsignedLong currentSlot;

  @Schema(type = "string", format = "uint64")
  public final UnsignedLong highestSlot;

  public SyncStatus(
      final UnsignedLong startingSlot,
      final UnsignedLong currentSlot,
      final UnsignedLong highestSlot) {
    this.startingSlot = startingSlot;
    this.currentSlot = currentSlot;
    this.highestSlot = highestSlot;
  }

  public SyncStatus(final tech.pegasys.teku.sync.SyncStatus syncStatus) {
    if (syncStatus != null) {
      this.startingSlot = syncStatus.getStartingSlot();
      this.currentSlot = syncStatus.getCurrentSlot();
      this.highestSlot = syncStatus.getHighestSlot();
    } else {
      startingSlot = null;
      currentSlot = null;
      highestSlot = null;
    }
  }
}
