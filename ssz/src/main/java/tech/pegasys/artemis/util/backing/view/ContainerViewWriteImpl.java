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

package tech.pegasys.artemis.util.backing.view;

import tech.pegasys.artemis.util.backing.ContainerViewRead;
import tech.pegasys.artemis.util.backing.ContainerViewWriteRef;
import tech.pegasys.artemis.util.backing.ViewRead;
import tech.pegasys.artemis.util.backing.ViewWrite;
import tech.pegasys.artemis.util.backing.cache.IntCache;
import tech.pegasys.artemis.util.backing.tree.TreeNode;

public class ContainerViewWriteImpl
    extends AbstractCompositeViewWrite<ContainerViewWriteImpl, ViewRead, ViewWrite>
    implements ContainerViewWriteRef {

  public ContainerViewWriteImpl(AbstractCompositeViewRead<?, ViewRead> backingImmutableView) {
    super(backingImmutableView);
  }

  @Override
  protected AbstractCompositeViewRead<?, ViewRead> createViewRead(
      TreeNode backingNode, IntCache<ViewRead> viewCache) {
    return new ContainerViewReadImpl(getType(), backingNode, viewCache);
  }

  @Override
  public ContainerViewRead commitChanges() {
    return (ContainerViewRead) super.commitChanges();
  }

  @Override
  public ViewWrite createWritableCopy() {
    return super.createWritableCopy();
  }

  @Override
  protected void checkIndex(int index, boolean set) {
    if (index >= size()) {
      throw new IndexOutOfBoundsException(
          "Invalid index " + index + " for container with size " + size());
    }
  }
}
