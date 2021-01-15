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

import tech.pegasys.teku.ssz.backing.ViewRead;
import tech.pegasys.teku.ssz.backing.tree.TreeNode;
import tech.pegasys.teku.ssz.backing.view.AbstractImmutableContainer;

/** Autogenerated by tech.pegasys.teku.ssz.backing.ContainersGenerator */
public class Container15<
        C extends Container15<C, V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14>,
        V0 extends ViewRead,
        V1 extends ViewRead,
        V2 extends ViewRead,
        V3 extends ViewRead,
        V4 extends ViewRead,
        V5 extends ViewRead,
        V6 extends ViewRead,
        V7 extends ViewRead,
        V8 extends ViewRead,
        V9 extends ViewRead,
        V10 extends ViewRead,
        V11 extends ViewRead,
        V12 extends ViewRead,
        V13 extends ViewRead,
        V14 extends ViewRead>
    extends AbstractImmutableContainer {

  protected Container15(
      ContainerType15<C, V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14> type) {
    super(type);
  }

  protected Container15(
      ContainerType15<C, V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14> type,
      TreeNode backingNode) {
    super(type, backingNode);
  }

  protected Container15(
      ContainerType15<C, V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13, V14> type,
      V0 arg0,
      V1 arg1,
      V2 arg2,
      V3 arg3,
      V4 arg4,
      V5 arg5,
      V6 arg6,
      V7 arg7,
      V8 arg8,
      V9 arg9,
      V10 arg10,
      V11 arg11,
      V12 arg12,
      V13 arg13,
      V14 arg14) {
    super(
        type, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12,
        arg13, arg14);
  }

  protected V0 getField0() {
    return getAny(0);
  }

  protected V1 getField1() {
    return getAny(1);
  }

  protected V2 getField2() {
    return getAny(2);
  }

  protected V3 getField3() {
    return getAny(3);
  }

  protected V4 getField4() {
    return getAny(4);
  }

  protected V5 getField5() {
    return getAny(5);
  }

  protected V6 getField6() {
    return getAny(6);
  }

  protected V7 getField7() {
    return getAny(7);
  }

  protected V8 getField8() {
    return getAny(8);
  }

  protected V9 getField9() {
    return getAny(9);
  }

  protected V10 getField10() {
    return getAny(10);
  }

  protected V11 getField11() {
    return getAny(11);
  }

  protected V12 getField12() {
    return getAny(12);
  }

  protected V13 getField13() {
    return getAny(13);
  }

  protected V14 getField14() {
    return getAny(14);
  }
}