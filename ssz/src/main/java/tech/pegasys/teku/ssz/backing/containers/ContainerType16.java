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
public abstract class ContainerType16<
        C extends SszContainer,
        V0 extends SszData,
        V1 extends SszData,
        V2 extends SszData,
        V3 extends SszData,
        V4 extends SszData,
        V5 extends SszData,
        V6 extends SszData,
        V7 extends SszData,
        V8 extends SszData,
        V9 extends SszData,
        V10 extends SszData,
        V11 extends SszData,
        V12 extends SszData,
        V13 extends SszData,
        V14 extends SszData,
        V15 extends SszData>
    extends ContainerViewType<C> {

  public static <
          C extends SszContainer,
          V0 extends SszData,
          V1 extends SszData,
          V2 extends SszData,
          V3 extends SszData,
          V4 extends SszData,
          V5 extends SszData,
          V6 extends SszData,
          V7 extends SszData,
          V8 extends SszData,
          V9 extends SszData,
          V10 extends SszData,
          V11 extends SszData,
          V12 extends SszData,
          V13 extends SszData,
          V14 extends SszData,
          V15 extends SszData>
      ContainerType16<C, V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15>
          create(
              ViewType<V0> fieldType0,
              ViewType<V1> fieldType1,
              ViewType<V2> fieldType2,
              ViewType<V3> fieldType3,
              ViewType<V4> fieldType4,
              ViewType<V5> fieldType5,
              ViewType<V6> fieldType6,
              ViewType<V7> fieldType7,
              ViewType<V8> fieldType8,
              ViewType<V9> fieldType9,
              ViewType<V10> fieldType10,
              ViewType<V11> fieldType11,
              ViewType<V12> fieldType12,
              ViewType<V13> fieldType13,
              ViewType<V14> fieldType14,
              ViewType<V15> fieldType15,
              BiFunction<
                      ContainerType16<
                          C, V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14, V15>,
                      TreeNode,
                      C>
                  instanceCtor) {
    return new ContainerType16<>(
        fieldType0,
        fieldType1,
        fieldType2,
        fieldType3,
        fieldType4,
        fieldType5,
        fieldType6,
        fieldType7,
        fieldType8,
        fieldType9,
        fieldType10,
        fieldType11,
        fieldType12,
        fieldType13,
        fieldType14,
        fieldType15) {
      @Override
      public C createFromBackingNode(TreeNode node) {
        return instanceCtor.apply(this, node);
      }
    };
  }

  protected ContainerType16(
      ViewType<V0> fieldType0,
      ViewType<V1> fieldType1,
      ViewType<V2> fieldType2,
      ViewType<V3> fieldType3,
      ViewType<V4> fieldType4,
      ViewType<V5> fieldType5,
      ViewType<V6> fieldType6,
      ViewType<V7> fieldType7,
      ViewType<V8> fieldType8,
      ViewType<V9> fieldType9,
      ViewType<V10> fieldType10,
      ViewType<V11> fieldType11,
      ViewType<V12> fieldType12,
      ViewType<V13> fieldType13,
      ViewType<V14> fieldType14,
      ViewType<V15> fieldType15) {

    super(
        List.of(
            fieldType0,
            fieldType1,
            fieldType2,
            fieldType3,
            fieldType4,
            fieldType5,
            fieldType6,
            fieldType7,
            fieldType8,
            fieldType9,
            fieldType10,
            fieldType11,
            fieldType12,
            fieldType13,
            fieldType14,
            fieldType15));
  }

  protected ContainerType16(
      String containerName,
      NamedType<V0> fieldNamedType0,
      NamedType<V1> fieldNamedType1,
      NamedType<V2> fieldNamedType2,
      NamedType<V3> fieldNamedType3,
      NamedType<V4> fieldNamedType4,
      NamedType<V5> fieldNamedType5,
      NamedType<V6> fieldNamedType6,
      NamedType<V7> fieldNamedType7,
      NamedType<V8> fieldNamedType8,
      NamedType<V9> fieldNamedType9,
      NamedType<V10> fieldNamedType10,
      NamedType<V11> fieldNamedType11,
      NamedType<V12> fieldNamedType12,
      NamedType<V13> fieldNamedType13,
      NamedType<V14> fieldNamedType14,
      NamedType<V15> fieldNamedType15) {

    super(
        containerName,
        List.of(
            fieldNamedType0,
            fieldNamedType1,
            fieldNamedType2,
            fieldNamedType3,
            fieldNamedType4,
            fieldNamedType5,
            fieldNamedType6,
            fieldNamedType7,
            fieldNamedType8,
            fieldNamedType9,
            fieldNamedType10,
            fieldNamedType11,
            fieldNamedType12,
            fieldNamedType13,
            fieldNamedType14,
            fieldNamedType15));
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

  @SuppressWarnings("unchecked")
  public ViewType<V3> getFieldType3() {
    return (ViewType<V3>) getChildType(3);
  }

  @SuppressWarnings("unchecked")
  public ViewType<V4> getFieldType4() {
    return (ViewType<V4>) getChildType(4);
  }

  @SuppressWarnings("unchecked")
  public ViewType<V5> getFieldType5() {
    return (ViewType<V5>) getChildType(5);
  }

  @SuppressWarnings("unchecked")
  public ViewType<V6> getFieldType6() {
    return (ViewType<V6>) getChildType(6);
  }

  @SuppressWarnings("unchecked")
  public ViewType<V7> getFieldType7() {
    return (ViewType<V7>) getChildType(7);
  }

  @SuppressWarnings("unchecked")
  public ViewType<V8> getFieldType8() {
    return (ViewType<V8>) getChildType(8);
  }

  @SuppressWarnings("unchecked")
  public ViewType<V9> getFieldType9() {
    return (ViewType<V9>) getChildType(9);
  }

  @SuppressWarnings("unchecked")
  public ViewType<V10> getFieldType10() {
    return (ViewType<V10>) getChildType(10);
  }

  @SuppressWarnings("unchecked")
  public ViewType<V11> getFieldType11() {
    return (ViewType<V11>) getChildType(11);
  }

  @SuppressWarnings("unchecked")
  public ViewType<V12> getFieldType12() {
    return (ViewType<V12>) getChildType(12);
  }

  @SuppressWarnings("unchecked")
  public ViewType<V13> getFieldType13() {
    return (ViewType<V13>) getChildType(13);
  }

  @SuppressWarnings("unchecked")
  public ViewType<V14> getFieldType14() {
    return (ViewType<V14>) getChildType(14);
  }

  @SuppressWarnings("unchecked")
  public ViewType<V15> getFieldType15() {
    return (ViewType<V15>) getChildType(15);
  }
}
