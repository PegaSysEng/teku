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
public abstract class ContainerType5<
        C extends SszContainer,
        V0 extends SszData,
        V1 extends SszData,
        V2 extends SszData,
        V3 extends SszData,
        V4 extends SszData>
    extends SszContainerSchema<C> {

  public static <
          C extends SszContainer,
          V0 extends SszData,
          V1 extends SszData,
          V2 extends SszData,
          V3 extends SszData,
          V4 extends SszData>
      ContainerType5<C, V0, V1, V2, V3, V4> create(
          SszSchema<V0> fieldType0,
          SszSchema<V1> fieldType1,
          SszSchema<V2> fieldType2,
          SszSchema<V3> fieldType3,
          SszSchema<V4> fieldType4,
          BiFunction<ContainerType5<C, V0, V1, V2, V3, V4>, TreeNode, C> instanceCtor) {
    return new ContainerType5<>(fieldType0, fieldType1, fieldType2, fieldType3, fieldType4) {
      @Override
      public C createFromBackingNode(TreeNode node) {
        return instanceCtor.apply(this, node);
      }
    };
  }

  protected ContainerType5(
      SszSchema<V0> fieldType0,
      SszSchema<V1> fieldType1,
      SszSchema<V2> fieldType2,
      SszSchema<V3> fieldType3,
      SszSchema<V4> fieldType4) {

    super(List.of(fieldType0, fieldType1, fieldType2, fieldType3, fieldType4));
  }

  protected ContainerType5(
      String containerName,
      NamedSchema<V0> fieldNamedSchema0,
      NamedSchema<V1> fieldNamedSchema1,
      NamedSchema<V2> fieldNamedSchema2,
      NamedSchema<V3> fieldNamedSchema3,
      NamedSchema<V4> fieldNamedSchema4) {

    super(
        containerName,
        List.of(
            fieldNamedSchema0, fieldNamedSchema1, fieldNamedSchema2, fieldNamedSchema3,
            fieldNamedSchema4));
  }

  @SuppressWarnings("unchecked")
  public SszSchema<V0> getFieldType0() {
    return (SszSchema<V0>) getChildSchema(0);
  }

  @SuppressWarnings("unchecked")
  public SszSchema<V1> getFieldType1() {
    return (SszSchema<V1>) getChildSchema(1);
  }

  @SuppressWarnings("unchecked")
  public SszSchema<V2> getFieldType2() {
    return (SszSchema<V2>) getChildSchema(2);
  }

  @SuppressWarnings("unchecked")
  public SszSchema<V3> getFieldType3() {
    return (SszSchema<V3>) getChildSchema(3);
  }

  @SuppressWarnings("unchecked")
  public SszSchema<V4> getFieldType4() {
    return (SszSchema<V4>) getChildSchema(4);
  }
}
