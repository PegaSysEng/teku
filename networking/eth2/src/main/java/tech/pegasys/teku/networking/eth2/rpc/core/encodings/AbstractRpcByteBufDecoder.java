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

package tech.pegasys.teku.networking.eth2.rpc.core.encodings;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import tech.pegasys.teku.networking.eth2.rpc.core.encodings.compression.exceptions.PayloadSmallerThanExpectedException;

public abstract class AbstractRpcByteBufDecoder<TMessage> implements RpcByteBufDecoder<TMessage> {

  private CompositeByteBuf compositeByteBuf = Unpooled.compositeBuffer();

  public synchronized Optional<TMessage> decodeOneMessage(ByteBuf in) {
    if (!in.isReadable()) {
      return Optional.empty();
    }
    compositeByteBuf.addComponent(true, in.retainedSlice());
    try {
      Optional<TMessage> outBuf;
      while (true) {
        int readerIndex = compositeByteBuf.readerIndex();
        outBuf = decodeOneImpl(compositeByteBuf);
        if (outBuf.isPresent()
            || readerIndex == compositeByteBuf.readerIndex()
            || compositeByteBuf.readableBytes() == 0) {
          break;
        }
      }
      if (outBuf.isPresent()) {
        in.skipBytes(in.readableBytes() - compositeByteBuf.readableBytes());
        compositeByteBuf.release();
        compositeByteBuf = Unpooled.compositeBuffer();
      } else {
        in.skipBytes(in.readableBytes());
      }
      return outBuf;
    } catch (Throwable t) {
      compositeByteBuf.release();
      compositeByteBuf = Unpooled.compositeBuffer();
      throw t;
    }
  }

  @Override
  public void complete() {
    if (compositeByteBuf.isReadable()) {
      compositeByteBuf.release();
      throw new PayloadSmallerThanExpectedException(
          "Rpc stream complete, but unprocessed data left: " + compositeByteBuf.readableBytes());
    }
  }

  protected abstract Optional<TMessage> decodeOneImpl(ByteBuf in);
}
