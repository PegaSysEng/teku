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

package tech.pegasys.teku.ssz.backing.tree;

import static java.lang.Integer.min;

/**
 * Util methods for binary tree generalized indexes manipulations See
 * https://github.com/ethereum/eth2.0-specs/blob/v1.0.0/ssz/merkle-proofs.md#generalized-merkle-tree-index
 * for more info on generalized indexes
 */
public class GIndexUtil {

  /** See {@link #gIdxCompare(long, long)} */
  public enum NodeRelation {
    Left,
    Right,
    Successor,
    Predecessor,
    Same
  }

  /**
   * The generalized index of either a root tree node or an index of a node relative to the node
   * itself. Equal to <code>1</code>
   */
  static final long SELF_G_INDEX = 1;

  /**
   * The generalized index (normally an index of non-existing node) of the leftmost possible node
   */
  static final long LEFTMOST_G_INDEX = gIdxLeftmostFrom(SELF_G_INDEX);
  /**
   * The generalized index (normally an index of non-existing node) of the rightmost possible node
   */
  static final long RIGHTMOST_G_INDEX = gIdxRightmostFrom(SELF_G_INDEX);

  /**
   * Indicates that a relative generalized index refers to the node itself
   *
   * @see #SELF_G_INDEX
   */
  public static boolean gIdxIsSelf(long generalizedIndex) {
    return generalizedIndex == SELF_G_INDEX;
  }

  /**
   * Indicates how the node with generalized index <code>idx1</code> relates to the node with
   * generalized index <code>idx2</code>:
   *
   * <ul>
   *   <li>{@link NodeRelation#Left}: idx1 is to the left of idx2
   *   <li>{@link NodeRelation#Right}: idx1 is to the right of idx2
   *   <li>{@link NodeRelation#Successor}: idx1 is the successor of idx2
   *   <li>{@link NodeRelation#Predecessor}: idx1 is the predecessor of idx2
   *   <li>{@link NodeRelation#Same}: idx1 is equal to idx2
   * </ul>
   */
  public static NodeRelation gIdxCompare(long idx1, long idx2) {
    long anchor1 = Long.highestOneBit(idx1);
    long anchor2 = Long.highestOneBit(idx2);
    int depth1 = Long.bitCount(anchor1 - 1);
    int depth2 = Long.bitCount(anchor2 - 1);
    int minDepth = min(depth1, depth2);
    long minDepthIdx1 = idx1 >>> (depth1 - minDepth);
    long minDepthIdx2 = idx2 >>> (depth2 - minDepth);
    if (minDepthIdx1 == minDepthIdx2) {
      if (depth1 < depth2) {
        return NodeRelation.Predecessor;
      } else if (depth1 > depth2) {
        return NodeRelation.Successor;
      } else {
        return NodeRelation.Same;
      }
    } else {
      if (minDepthIdx1 < minDepthIdx2) {
        return NodeRelation.Left;
      } else {
        return NodeRelation.Right;
      }
    }
  }

  /**
   * Returns the depth of the node denoted by the supplied generalized index. E.g. the depth of the
   * {@link #SELF_G_INDEX} would be 0
   */
  public static int gIdxGetDepth(long generalizedIndex) {
    assert generalizedIndex >= 1;
    long anchor = Long.highestOneBit(generalizedIndex);
    return Long.bitCount(anchor - 1);
  }

  /**
   * Returns the generalized index of the left child of the node with specified generalized index
   * E.g. the result when passing {@link #SELF_G_INDEX} would be <code>10</code>
   */
  public static long gIdxLeftGIndex(long generalizedIndex) {
    return gIdxChildGIndex(generalizedIndex, 0, 1);
  }

  /**
   * Returns the generalized index of the right child of the node with specified generalized index
   * E.g. the result when passing {@link #SELF_G_INDEX} would be <code>11</code>
   */
  public static long gIdxRightGIndex(long generalizedIndex) {
    return gIdxChildGIndex(generalizedIndex, 1, 1);
  }

  /**
   * More generic variant of methods {@link #gIdxLeftGIndex(long)} {@link #gIdxRightGIndex(long)}
   * Calculates the generalized index of a node's <code>childIdx</code> successor at depth <code>
   * childDepth</code> (depth relative to the original node). Note that <code>childIdx</code> is not
   * the generalized index but index number of child.
   *
   * <p>For example:
   *
   * <ul>
   *   <li><code>gIdxChildGIndex(SELF_G_INDEX, 0, 2) == 100</code>
   *   <li><code>gIdxChildGIndex(SELF_G_INDEX, 1, 2) == 101</code>
   *   <li><code>gIdxChildGIndex(SELF_G_INDEX, 2, 2) == 110</code>
   *   <li><code>gIdxChildGIndex(SELF_G_INDEX, 3, 2) == 111</code>
   *   <li><code>gIdxChildGIndex(SELF_G_INDEX, 4, 2) is invalid cause there are just 4 successors
   *   at depth 2</code>
   *   <li><code>gIdxChildGIndex(anyIndex, 0, 1) == gIdxLeftGIndex(anyIndex)</code>
   *   <li><code>gIdxChildGIndex(anyIndex, 1, 1) == gIdxRightGIndex(anyIndex)</code>
   * </ul>
   */
  public static long gIdxChildGIndex(long generalizedIndex, int childIdx, int childDepth) {
    assert generalizedIndex >= 1;
    assert childIdx < (1 << childDepth);
    return (generalizedIndex << childDepth) | childIdx;
  }

  /**
   * Returns the generalized index (normally an index of non-existing node) of the leftmost possible
   * successor of this node
   */
  public static long gIdxLeftmostFrom(long fromGeneralizedIndex) {
    assert fromGeneralizedIndex >= 1;
    int nodeDepth = TreeUtil.treeDepth(fromGeneralizedIndex);
    assert nodeDepth <= 63;
    return fromGeneralizedIndex << (63 - nodeDepth);
  }

  /**
   * Returns the generalized index (normally an index of non-existing node) of the rightmost
   * possible successor of this node
   */
  public static long gIdxRightmostFrom(long fromGeneralizedIndex) {
    assert fromGeneralizedIndex >= 1;
    int nodeDepth = TreeUtil.treeDepth(fromGeneralizedIndex);
    assert nodeDepth <= 63;
    int shiftN = 63 - nodeDepth;
    return (fromGeneralizedIndex << shiftN) | ((1L << shiftN) - 1);
  }

  /**
   * Returns the index number (not a generalized index) of a node at depth <code>childDepth</code>
   * which is a predecessor of or equal to the node at <code>generalizedIndex</code>
   *
   * <p>For example:
   *
   * <ul>
   *   <li><code>gIdxGetChildIndex(LEFTMOST_G_INDEX, anyDepth) == 0</code>
   *   <li><code>gIdxGetChildIndex(0b1100, 2) == 2</code>
   *   <li><code>gIdxGetChildIndex(0b1101, 2) == 2</code>
   *   <li><code>gIdxGetChildIndex(0b1110, 2) == 3</code>
   *   <li><code>gIdxGetChildIndex(0b1111, 2) == 3</code>
   *   <li><code>gIdxGetChildIndex(0b11, 2)</code> call would be invalid cause node with index 0b11
   *       is at depth 1
   * </ul>
   */
  public static int gIdxGetChildIndex(long generalizedIndex, int childDepth) {
    long anchor = Long.highestOneBit(generalizedIndex);
    int indexBitCount = Long.bitCount(anchor - 1);
    if (indexBitCount < childDepth) {
      throw new IllegalArgumentException(
          "Generalized index " + generalizedIndex + " is upper than depth " + childDepth);
    }
    long generalizedIndexWithoutAnchor = generalizedIndex ^ anchor;
    return (int) (generalizedIndexWithoutAnchor >>> (indexBitCount - childDepth));
  }

  /**
   * Returns the generalized index of the node at <code>generalizedIndex</code> relative to its
   * predecessor at depth <code>childDepth</code> For example:
   *
   * <ul>
   *   <li><code>gIdxGetRelativeGIndex(0b1100, 2) == 0b10</code>
   *   <li><code>gIdxGetChildIndex(0b1101, 2) == 0b11</code>
   *   <li><code>gIdxGetChildIndex(0b1110, 2) == 0b10</code>
   *   <li><code>gIdxGetChildIndex(0b1111, 3) == SELF_G_INDEX</code>
   *   <li><code>gIdxGetChildIndex(0b11, 2)</code> call would be invalid cause node with index 0b11
   *       is at depth 1
   * </ul>
   */
  public static long gIdxGetRelativeGIndex(long generalizedIndex, int childDepth) {
    long anchor = Long.highestOneBit(generalizedIndex);
    long pivot = anchor >>> childDepth;
    if (pivot == 0) {
      throw new IllegalArgumentException(
          "Generalized index " + generalizedIndex + " is upper than depth " + childDepth);
    }
    return (generalizedIndex & (pivot - 1)) | pivot;
  }
}
