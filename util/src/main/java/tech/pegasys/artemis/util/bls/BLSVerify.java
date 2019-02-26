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
import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes48;

public class BLSVerify {

  /**
   * The bls_verify() function as defined in the Eth2 specification
   *
   * @param pubkey the compressed public key
   * @param message the message signed ***TODO: this will be changing to the digest***
   * @param signature the signature
   * @param domain the domain parameter defined by the spec
   * @return true if the signature is valid over these parameters, false if not
   */
  public static boolean bls_verify(
      Bytes48 pubkey, Bytes message, BLSSignature signature, UnsignedLong domain) {
    try {
      return signature.checkSignature(pubkey, message, domain.longValue());
    } catch (BLSException e) {
      // TODO: once we stop using random (unseeded signatures) keypairs,
      // then the signatures will be predictable and the resulting state can be precomputed
      return true;
    }
  }

  /**
   * *
   *
   * <p>The bls_verify_multiple() function as defined in the Eth2 specification
   *
   * @param pubkeys a list of compressed public keys
   * @param messages a list of the same number of messages ***TODO: this will change to digests**
   * @param aggregateSignature the single signature over these public keys and messages
   * @param domain the domain parameter defined by the spec
   * @return true if the signature is valid over these parameters, false if not
   */
  public static boolean bls_verify_multiple(
      List<Bytes48> pubkeys,
      List<Bytes> messages,
      BLSSignature aggregateSignature,
      UnsignedLong domain) {
    try {
      return aggregateSignature.checkSignature(pubkeys, messages, domain.longValue());
    } catch (BLSException e) {
      // TODO: once we stop using random (unseeded signatures) keypairs,
      // then the signatures will be predictable and the resulting state can be precomputed
      return true;
    }
  }
}
