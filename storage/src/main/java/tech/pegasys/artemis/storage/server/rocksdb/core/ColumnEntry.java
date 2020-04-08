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

package tech.pegasys.artemis.storage.server.rocksdb.core;

import java.util.Objects;

public class ColumnEntry<K, V> {
  private final K key;
  private final V value;

  private ColumnEntry(final K key, final V value) {
    this.key = key;
    this.value = value;
  }

  public static <K, V> ColumnEntry<K, V> create(final K key, final V value) {
    return new ColumnEntry<>(key, value);
  }

  public K getKey() {
    return key;
  }

  public V getValue() {
    return value;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ColumnEntry)) {
      return false;
    }
    final ColumnEntry<?, ?> other = (ColumnEntry<?, ?>) obj;
    return Objects.equals(getKey(), other.getKey()) && Objects.equals(getValue(), other.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }
}
