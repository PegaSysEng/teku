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

package pegasys.artemis.reference.bls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.errorprone.annotations.MustBeClosed;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes48;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pegasys.artemis.reference.TestSuite;
import tech.pegasys.artemis.util.mikuli.G2Point;

class g2_compressed extends TestSuite {

  @ParameterizedTest(name = "{index}. message hash to G2 compressed {0} -> {1}")
  @MethodSource("readMessageHashG2Compressed")
  void testMessageHashToG2Compressed(
      LinkedHashMap<String, String> input, ArrayList<String> output) {

    Bytes domain = Bytes.fromHexString(input.get("domain"));
    Bytes message = Bytes.fromHexString(input.get("message"));

    G2Point actual = G2Point.hashToG2(message, domain);
    Bytes48 xReExpected = Bytes48.leftPad(Bytes.fromHexString(output.get(0)));
    Bytes48 xImExpected = Bytes48.leftPad(Bytes.fromHexString(output.get(1)));
    Bytes expectedBytes = Bytes.concatenate(xReExpected, xImExpected);
    Bytes actualBytes = actual.toBytesCompressed();

    assertEquals(expectedBytes, actualBytes);
  }

  @MustBeClosed
  private static Stream<Arguments> readMessageHashG2Compressed() throws IOException {
    return findBLSTests("**/g2_compressed.yaml", "test_cases");
  }
}
