/*
 * Copyright 2018 ConsenSys AG.
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

package net.consensys.beaconchain.ethereum.rlp;

import net.consensys.beaconchain.util.bytes.Bytes32;
import net.consensys.beaconchain.util.bytes.BytesValue;
import net.consensys.beaconchain.util.uint.UInt256;
import net.consensys.beaconchain.util.uint.UInt256Value;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * An input used to decode data in RLP encoding.
 *
 * <p>
 * An RLP "value" is fundamentally an {@code Item} defined the following way:
 *
 * <pre>
 *   Item ::= List | Bytes
 *   List ::= [ Item, ... , Item ]
 *   Bytes ::= a binary value (comprised of an arbitrary number of bytes).
 * </pre>
 *
 * In other words, RLP encodes binary data organized in arbitrary nested lists.
 *
 * <p>
 * A {@link RLPInput} thus provides methods to decode both lists and binary values. A list in the
 * input is "entered" by calling {@link #enterList()} and left by calling {@link #leaveList()}.
 * Binary values can be read directly with {@link #readBytesValue()} ()}, but the {@link RLPInput}
 * interface provides a wealth of convenience methods to read specific types of data that are in
 * specific encoding.
 *
 * <p>
 * Amongst the methods to read binary data, some methods are provided to read "scalar". A scalar
 * should simply be understood as a positive integer that is encoded with no leading zeros. In other
 * word, a method like {@link #readLongScalar()} does not expect an encoded value of exactly 8 bytes
 * (by opposition to {@link #readLong}), but rather one that is "up to" 8 bytes.
 *
 * @see BytesValueRLPInput for a {@link RLPInput} that decode an RLP encoded value stored in a
 *      {@link BytesValue}.
 */
public interface RLPInput {

  /**
   * Whether the input has been already fully decoded (has no more data to read).
   *
   * @return {@code false} if the input has more data to read, {@code true} otherwise.
   */
  boolean isDone();

  /**
   * Whether the next element to read from this input is a list.
   *
   * @return {@code true} if the input is not done and the next item to read is a list.
   */
  boolean nextIsList();

  /**
   * Whether the next element to read from this input is an RLP "null" (that is,
   * {@link BytesValue#EMPTY}).
   *
   * @return {@code true} if the input is not done and the next item to read is an empty value.
   */
  boolean nextIsNull();

  /**
   * Returns the payload size of the next item
   *
   * @return the payload size of the next item
   */
  int nextSize();

  /**
   * Whether the input is at the end of a currently entered list, that is if {@link #leaveList()}
   * should be the next method called.
   *
   * @return Whether all elements of the current list have been read but said list haven't been
   *         "left" yet.
   */
  boolean isEndOfCurrentList();

  /**
   * Skips the next item to read in the input.
   *
   * <p>
   * Note that if the next item is a list, the whole list is skipped.
   */
  void skipNext();

  /**
   * If the next item to read is a list, enter that list, placing the input on the first item of
   * that list.
   *
   * @return The number of item of the entered list.
   * @throws RLPException if the next item to read from this input is not a list, or the input is
   *         corrupted.
   */
  int enterList();

  /**
   * Exits the current list after all its items have been consumed.
   *
   * <p>
   * This method is equivalent to calling {@link #leaveList(boolean)} with value <tt>false</tt>.
   *
   * <p>
   * Note that this method technically doesn't consume any input but must be called after having
   * read the last element of a list. This allow to ensure the structure of the input is indeed the
   * one expected.
   *
   * @throws RLPException if the current list is not finished (it has more items).
   */
  void leaveList();

  /**
   * Exits the current list, allowing the caller to ignore any remaining unconsumed elements.
   *
   * <p>
   * Note that this method technically doesn't consume any input but must be called after having
   * read the last element of a list. This allow to ensure the structure of the input is indeed the
   * one expected.
   *
   * @param ignoreRest Whether to ignore any remaining elements in the list. If elements remain and
   *        this parameter is <tt>false</tt>, an exception will be thrown.
   *
   * @throws RLPException if the current list is not finished (it has more items), if
   *         <tt>ignoreRest</tt> is <tt>false</tt>.
   */
  void leaveList(boolean ignoreRest);

  /**
   * Reads a scalar from the input and return is as a long value.
   *
   * @return The next scalar item of this input as a long value.
   * @throws RLPException if the next item to read is a list, the input is at the end of its current
   *         list (and {@link #leaveList()} hasn't been called) or if the next item is either too
   *         big to fit a long or has leading zeros.
   */
  long readLongScalar();

  /**
   * Reads a scalar from the input and return is as an int value.
   *
   * @return The next scalar item of this input as an int value.
   * @throws RLPException if the next item to read is a list, the input is at the end of its current
   *         list (and {@link #leaveList()} hasn't been called) or if the next item is either too
   *         big to fit a long or has leading zeros.
   */
  int readIntScalar();

  /**
   * Reads a scalar from the input and return is as a {@link BigInteger}.
   *
   * @return The next scalar item of this input as a {@link BigInteger}.
   * @throws RLPException if the next item to read is a list, the input is at the end of its current
   *         list (and {@link #leaveList()} hasn't been called) or if the next item has leading
   *         zeros.
   */
  BigInteger readBigIntegerScalar();

  /**
   * Reads a scalar from the input and return is as a {@link UInt256}.
   *
   * @return The next scalar item of this input as a {@link UInt256}.
   * @throws RLPException if the next item to read is a list, the input is at the end of its current
   *         list (and {@link #leaveList()} hasn't been called) or if the next item is either too
   *         big to fit a {@link UInt256} or has leading zeros.
   */
  UInt256 readUInt256Scalar();

  /**
   * Reads a scalar of maximum 32 bytes from the input and pass it to the provided value to create a
   * corresponding {@link UInt256Value}.
   *
   * <p>
   * Note that for convenience, any exception thrown by the provided method will be wrapped in a
   * {@link RLPException} (it is considered as a "decoding error").
   *
   * @param bytesWrapper A function that provided a 32 bytes value creates a specific 32 bytes
   *        unsigned integer value.
   * @param <T> Type of the value created, which must be 32 bytes unsigned integer variant.
   * @return The value created from applying {@code bytesWrapper} to the scalar read from that input
   *         (eventually padded to fit 32 bytes).
   * @throws RLPException if the next item to read is a list, the input is at the end of its current
   *         list (and {@link #leaveList()} hasn't been called) or if the next item is either too
   *         big to fit a {@link UInt256} or has leading zeros.
   */
  <T extends UInt256Value<T>> T readUInt256Scalar(Function<Bytes32, T> bytesWrapper);

  /**
   * Reads the next item of this input (which must be exactly 1 byte) as a byte.
   *
   * @return The byte corresponding to the next item of this input.
   * @throws RLPException if the next item to read is a list, the input is at the end of its current
   *         list (and {@link #leaveList()} hasn't been called) or if the next item is not a single
   *         byte long.
   */
  byte readByte();

  /**
   * Reads the next item of this input (which must be exactly 2-bytes) as a (signed) short.
   *
   * @return The short corresponding to the next item of this input.
   * @throws RLPException if the next item to read is a list, the input is at the end of its current
   *         list (and {@link #leaveList()} hasn't been called) or if the next item is not 2-bytes.
   */
  short readShort();

  /**
   * Reads the next item of this input (which must be exactly 4-bytes) as a (signed) int.
   *
   * @return The int corresponding to the next item of this input.
   * @throws RLPException if the next item to read is a list, the input is at the end of its current
   *         list (and {@link #leaveList()} hasn't been called) or if the next item is not 4-bytes.
   */
  int readInt();

  /**
   * Reads the next item of this input (which must be exactly 8-bytes) as a (signed) long.
   *
   * @return The long corresponding to the next item of this input.
   * @throws RLPException if the next item to read is a list, the input is at the end of its current
   *         list (and {@link #leaveList()} hasn't been called) or if the next item is not 8-bytes.
   */
  long readLong();

  /**
   * Reads the next item of this input (which must be exactly 1 byte) as an unsigned byte.
   *
   * @return The value of the next item interpreted as an unsigned byte.
   * @throws RLPException if the next item to read is a list, the input is at the end of its current
   *         list (and {@link #leaveList()} hasn't been called) or if the next item is not a single
   *         byte long.
   */
  default int readUnsignedByte() {
    return readByte() & 0xFF;
  }

  /**
   * Reads the next item of this input (which must be exactly 2-bytes) as an unsigned short.
   *
   * @return The value of the next item interpreted as an unsigned short.
   * @throws RLPException if the next item to read is a list, the input is at the end of its current
   *         list (and {@link #leaveList()} hasn't been called) or if the next item is not 2-bytes.
   */
  default int readUnsignedShort() {
    return readShort() & 0xFFFF;
  }

  /**
   * Reads the next item of this input (which must be exactly 4-bytes) as an unsigned int.
   *
   * @return The value of the next item interpreted as an unsigned int.
   * @throws RLPException if the next item to read is a list, the input is at the end of its current
   *         list (and {@link #leaveList()} hasn't been called) or if the next item is not 4-bytes.
   */
  default long readUnsignedInt() {
    return ((long) readInt()) & 0xFFFFFFFFL;
  }

  /**
   * Reads an inet address from this input.
   *
   * @return The inet address corresponding to the next item of this input.
   * @throws RLPException if the next item to read is a list, the input is at the end of its current
   *         list (and {@link #leaveList()} hasn't been called) or if the next item is neither 4 nor
   *         16 bytes.
   */
  InetAddress readInetAddress();

  /**
   * Reads the next item of this input (assuming it is not a list).
   *
   * @return The next item read of this input.
   * @throws RLPException if the next item to read is a list or the input is at the end of its
   *         current list (and {@link #leaveList()} hasn't been called).
   */
  BytesValue readBytesValue();

  /**
   * Reads the next item of this input (assuming it is not a list) that must be exact 32 bytes.
   *
   * @return The next item read of this input.
   * @throws RLPException if the next item to read is a list, the input is at the end of its current
   *         list (and {@link #leaveList()} hasn't been called) or the next element is not exactly
   *         32 bytes.
   */
  Bytes32 readBytes32();

  /**
   * Reads the next iterm of this input (assuming it is not a list) and transform it with the
   * provided mapping function.
   *
   * <p>
   * Note that the only benefit of this method over calling the mapper function on the result of
   * {@link #readBytesValue()} is that any error thrown by the mapper will be wrapped by a
   * {@link RLPException}, which can make error handling more convenient (having a particular
   * decoded value not match what is expected is not fundamentally different from trying to read an
   * unsigned short from an item with strictly more or less than 2 bytes).
   *
   * @param mapper The mapper to apply to the read value.
   * @param <T> The type of the result.
   * @return The next item read from this input, mapped through {@code mapper}.
   * @throws RLPException if the next item to read is a list, the input is at the end of its current
   *         list (and {@link #leaveList()} hasn't been called) or {@code mapper} throws an
   *         exception when applied.
   */
  <T> T readBytesValue(Function<BytesValue, T> mapper);


  /**
   * Returns the current element as a standalone RLP element.
   *
   * <p>
   * This method is useful to extract self-contained RLP elements from a list, so they can be
   * processed individually later.
   *
   * @return The current element as a standalone RLP input element.
   */
  RLPInput readAsRlp();

  /**
   * Returns a raw {@link BytesValue} representation of this RLP.
   *
   * @return The raw RLP.
   */
  BytesValue raw();

  /**
   * Resets this RLP input to the start.
   */
  void reset();

  /**
   * Reads a full list from the input given a method that knows how to read its elements.
   *
   * @param valueReader A method that can decode a single list element.
   * @param <T> The type of the elements of the decoded list.
   * @return The next list of this input, where elements are decoded using {@code valueReader}.
   * @throws RLPException is the next item to read is not a list, of if any error happens when
   *         applying {@code valueReader} to read elements of the list.
   */
  default <T> List<T> readList(Function<RLPInput, T> valueReader) {
    int size = enterList();
    List<T> res = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      try {
        res.add(valueReader.apply(this));
      } catch (Exception e) {
        throw new RLPException(String.format(
            "Error applying element decoding function on " + "element %d of the list", i), e);
      }
    }
    leaveList();
    return res;
  }
}
