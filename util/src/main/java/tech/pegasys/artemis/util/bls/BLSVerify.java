/*
 * Copyright 2019 ConsenSys AG.
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

package tech.pegasys.artemis.util.bls;

import com.google.common.primitives.UnsignedLong;
import java.util.List;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.bytes.Bytes48;

public class BLSVerify {

  public static boolean bls_verify(
      Bytes48 pubkey, Bytes32 message, Signature signature, UnsignedLong domain) {
    // todo
    return true;
  }

  public static boolean bls_verify_multiple(
      List<Bytes48> pubkeys,
      List<Bytes32> messages,
      Signature aggregateSignature,
      UnsignedLong domain) {
    // todo
    return true;
  }

  public static boolean bls_verify_multiple(
      List<Bytes48> pubkeys,
      List<Bytes32> messages,
      Signature aggregateSignature,
      UnsignedLong domain) {
    // todo
    return true;
  }

  public static Bytes48 bls_aggregate_pubkeys(List<Bytes48> pubkeys) {
    return Bytes48.ZERO;
  }
}
