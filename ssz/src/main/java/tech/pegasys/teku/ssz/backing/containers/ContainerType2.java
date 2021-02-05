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

package tech.pegasys.teku.ssz.backing.containers;

import java.util.List;
import java.util.function.BiFunction;
import tech.pegasys.teku.ssz.backing.SszContainer;
import tech.pegasys.teku.ssz.backing.SszData;
import tech.pegasys.teku.ssz.backing.tree.TreeNode;
import tech.pegasys.teku.ssz.backing.type.SszContainerSchema;
import tech.pegasys.teku.ssz.backing.type.SszSchema;

/** Autogenerated by tech.pegasys.teku.ssz.backing.ContainersGenerator */
public abstract class ContainerType2<C extends SszContainer, V0 extends SszData, V1 extends SszData>
    extends SszContainerSchema<C> {

  public static <C extends SszContainer, V0 extends SszData, V1 extends SszData>
      ContainerType2<C, V0, V1> create(
          SszSchema<V0> fieldType0,
          SszSchema<V1> fieldType1,
          BiFunction<ContainerType2<C, V0, V1>, TreeNode, C> instanceCtor) {
    return new ContainerType2<>(fieldType0, fieldType1) {
      @Override
      public C createFromBackingNode(TreeNode node) {
        return instanceCtor.apply(this, node);
      }
    };
  }

  protected ContainerType2(SszSchema<V0> fieldType0, SszSchema<V1> fieldType1) {

    super(List.of(fieldType0, fieldType1));
  }

  protected ContainerType2(
      String containerName, NamedSchema<V0> fieldNamedSchema0, NamedSchema<V1> fieldNamedSchema1) {

    super(containerName, List.of(fieldNamedSchema0, fieldNamedSchema1));
  }

  @SuppressWarnings("unchecked")
  public SszSchema<V0> getFieldType0() {
    return (SszSchema<V0>) getChildSchema(0);
  }

  @SuppressWarnings("unchecked")
  public SszSchema<V1> getFieldType1() {
    return (SszSchema<V1>) getChildSchema(1);
  }
}
