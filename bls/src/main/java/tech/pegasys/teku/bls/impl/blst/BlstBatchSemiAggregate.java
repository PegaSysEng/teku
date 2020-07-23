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

package tech.pegasys.teku.bls.impl.blst;

import tech.pegasys.teku.bls.BatchSemiAggregate;
import tech.pegasys.teku.bls.impl.blst.swig.BLST_ERROR;
import tech.pegasys.teku.bls.impl.blst.swig.blst;
import tech.pegasys.teku.bls.impl.blst.swig.pairing;

public final class BlstBatchSemiAggregate implements BatchSemiAggregate {
  private final pairing ctx;
  private boolean released = false;

  BlstBatchSemiAggregate(pairing ctx) {
    this.ctx = ctx;
  }

  pairing getCtx() {
    return ctx;
  }

  void release() {
    if (released) throw new IllegalStateException("Attempting to use disposed BatchSemiAggregate");
    released = true;
    ctx.delete();
  }

  void mergeWith(BlstBatchSemiAggregate other) {
    BLST_ERROR ret = blst.pairing_merge(getCtx(), other.getCtx());
    if (ret != BLST_ERROR.BLST_SUCCESS) {
      throw new IllegalStateException("Error merging Blst pairing contexts: " + ret);
    }
  }
}
