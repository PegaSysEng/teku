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

package tech.pegasys.teku.datastructures.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.api.Test;
import tech.pegasys.teku.datastructures.util.DataStructureUtil;
import tech.pegasys.teku.datastructures.util.SimpleOffsetSerializer;

class CheckpointTest {
  private final DataStructureUtil dataStructureUtil = new DataStructureUtil();

  @Test
  void roundTripViaSsz() {
    Checkpoint checkpoint = dataStructureUtil.randomCheckpoint();
    Bytes checkpointSerialized = checkpoint.sszSerialize();
    Checkpoint newCheckpoint =
        SimpleOffsetSerializer.deserialize(checkpointSerialized, Checkpoint.class);
    assertEquals(checkpoint, newCheckpoint);
  }
}
