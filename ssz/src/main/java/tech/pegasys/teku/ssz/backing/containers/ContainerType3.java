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
import tech.pegasys.teku.ssz.backing.type.ContainerViewType;
import tech.pegasys.teku.ssz.backing.type.ViewType;

/** Autogenerated by tech.pegasys.teku.ssz.backing.ContainersGenerator */
public abstract class ContainerType3<
        C extends SszContainer, V0 extends SszData, V1 extends SszData, V2 extends SszData>
    extends ContainerViewType<C> {

  public static <
          C extends SszContainer,
          V0 extends SszData,
          V1 extends SszData,
          V2 extends SszData>
      ContainerType3<C, V0, V1, V2> create(
          ViewType<V0> fieldType0,
          ViewType<V1> fieldType1,
          ViewType<V2> fieldType2,
          BiFunction<ContainerType3<C, V0, V1, V2>, TreeNode, C> instanceCtor) {
    return new ContainerType3<>(fieldType0, fieldType1, fieldType2) {
      @Override
      public C createFromBackingNode(TreeNode node) {
        return instanceCtor.apply(this, node);
      }
    };
  }

  protected ContainerType3(
      ViewType<V0> fieldType0, ViewType<V1> fieldType1, ViewType<V2> fieldType2) {

    super(List.of(fieldType0, fieldType1, fieldType2));
  }

  protected ContainerType3(
      String containerName,
      NamedType<V0> fieldNamedType0,
      NamedType<V1> fieldNamedType1,
      NamedType<V2> fieldNamedType2) {

    super(containerName, List.of(fieldNamedType0, fieldNamedType1, fieldNamedType2));
  }

  @SuppressWarnings("unchecked")
  public ViewType<V0> getFieldType0() {
    return (ViewType<V0>) getChildType(0);
  }

  @SuppressWarnings("unchecked")
  public ViewType<V1> getFieldType1() {
    return (ViewType<V1>) getChildType(1);
  }

  @SuppressWarnings("unchecked")
  public ViewType<V2> getFieldType2() {
    return (ViewType<V2>) getChildType(2);
  }
}
