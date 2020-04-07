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

package tech.pegasys.artemis.datastructures.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.google.common.primitives.UnsignedLong;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.datastructures.util.DataStructureUtil;
import tech.pegasys.artemis.bls.bls.BLSPublicKey;
import tech.pegasys.artemis.bls.bls.BLSSignature;

class DepositDataTest {
  private final DataStructureUtil dataStructureUtil = new DataStructureUtil();
  private BLSPublicKey pubkey = dataStructureUtil.randomPublicKey();
  private Bytes32 withdrawalCredentials = dataStructureUtil.randomBytes32();
  private UnsignedLong amount = dataStructureUtil.randomUnsignedLong();
  private BLSSignature signature = dataStructureUtil.randomSignature();

  private DepositData depositData =
      new DepositData(pubkey, withdrawalCredentials, amount, signature);

  @Test
  void equalsReturnsTrueWhenObjectsAreSame() {
    DepositData testDepositInput = depositData;

    assertEquals(depositData, testDepositInput);
  }

  @Test
  void equalsReturnsTrueWhenObjectFieldsAreEqual() {
    DepositData testDepositInput =
        new DepositData(pubkey, withdrawalCredentials, amount, signature);

    assertEquals(depositData, testDepositInput);
  }

  @Test
  void equalsReturnsFalseWhenPubkeysAreDifferent() {
    BLSPublicKey differentPublicKey = dataStructureUtil.randomPublicKey();
    DepositData testDepositInput =
        new DepositData(differentPublicKey, withdrawalCredentials, amount, signature);

    assertNotEquals(pubkey, differentPublicKey);
    assertNotEquals(depositData, testDepositInput);
  }

  @Test
  void equalsReturnsFalseWhenWithdrawalCredentialsAreDifferent() {
    DepositData testDepositInput =
        new DepositData(pubkey, withdrawalCredentials.not(), amount, signature);

    assertNotEquals(depositData, testDepositInput);
  }

  @Test
  void equalsReturnsFalseWhenProofsOfPosessionAreDifferent() {
    BLSSignature differentSignature = dataStructureUtil.randomSignature();
    DepositData testDepositInput =
        new DepositData(pubkey, withdrawalCredentials, amount, differentSignature);

    assertNotEquals(signature, differentSignature);
    assertNotEquals(depositData, testDepositInput);
  }

  @Test
  void roundtripSSZ() {
    Bytes sszDepositInputBytes = depositData.toBytes();
    assertEquals(depositData, DepositData.fromBytes(sszDepositInputBytes));
  }
}
