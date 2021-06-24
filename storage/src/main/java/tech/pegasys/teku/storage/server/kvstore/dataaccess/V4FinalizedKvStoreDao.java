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

package tech.pegasys.teku.storage.server.kvstore.dataaccess;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.MustBeClosed;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.datastructures.blocks.SignedBeaconBlock;
import tech.pegasys.teku.spec.datastructures.blocks.SlotAndBlockRoot;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconState;
import tech.pegasys.teku.storage.server.kvstore.ColumnEntry;
import tech.pegasys.teku.storage.server.kvstore.KvStoreAccessor;
import tech.pegasys.teku.storage.server.kvstore.KvStoreAccessor.KvStoreTransaction;
import tech.pegasys.teku.storage.server.kvstore.schema.KvStoreColumn;
import tech.pegasys.teku.storage.server.kvstore.schema.KvStoreVariable;
import tech.pegasys.teku.storage.server.kvstore.schema.SchemaFinalized;

public class V4FinalizedKvStoreDao implements KvStoreFinalizedDao {
  private final KvStoreAccessor db;
  private final SchemaFinalized schema;
  private final UInt64 stateStorageFrequency;

  public V4FinalizedKvStoreDao(
      final KvStoreAccessor db, final SchemaFinalized schema, final long stateStorageFrequency) {
    this.db = db;
    this.schema = schema;
    this.stateStorageFrequency = UInt64.valueOf(stateStorageFrequency);
  }

  @Override
  public void close() throws Exception {
    db.close();
  }

  @Override
  public Optional<SignedBeaconBlock> getFinalizedBlockAtSlot(final UInt64 slot) {
    return db.get(schema.getColumnFinalizedBlocksBySlot(), slot);
  }

  @Override
  public Optional<UInt64> getEarliestFinalizedBlockSlot() {
    return db.getFirstEntry(schema.getColumnFinalizedBlocksBySlot()).map(ColumnEntry::getKey);
  }

  @Override
  public Optional<SignedBeaconBlock> getEarliestFinalizedBlock() {
    return db.getFirstEntry(schema.getColumnFinalizedBlocksBySlot()).map(ColumnEntry::getValue);
  }

  @Override
  public Optional<SignedBeaconBlock> getLatestFinalizedBlockAtSlot(final UInt64 slot) {
    return db.getFloorEntry(schema.getColumnFinalizedBlocksBySlot(), slot)
        .map(ColumnEntry::getValue);
  }

  @Override
  public Set<SignedBeaconBlock> getNonCanonicalBlocksAtSlot(final UInt64 slot) {
    Optional<Set<Bytes32>> maybeRoots = db.get(schema.getColumnNonCanonicalRootsBySlot(), slot);
    return maybeRoots.stream()
        .flatMap(Collection::stream)
        .flatMap(root -> db.get(schema.getColumnNonCanonicalBlocksByRoot(), root).stream())
        .collect(Collectors.toSet());
  }

  @Override
  public Optional<BeaconState> getLatestAvailableFinalizedState(final UInt64 maxSlot) {
    return db.getFloorEntry(schema.getColumnFinalizedStatesBySlot(), maxSlot)
        .map(ColumnEntry::getValue);
  }

  @Override
  @MustBeClosed
  public Stream<SignedBeaconBlock> streamFinalizedBlocks(
      final UInt64 startSlot, final UInt64 endSlot) {
    return db.stream(schema.getColumnFinalizedBlocksBySlot(), startSlot, endSlot)
        .map(ColumnEntry::getValue);
  }

  @Override
  public Optional<UInt64> getSlotForFinalizedBlockRoot(final Bytes32 blockRoot) {
    return db.get(schema.getColumnSlotsByFinalizedRoot(), blockRoot);
  }

  @Override
  public Optional<UInt64> getSlotForFinalizedStateRoot(final Bytes32 stateRoot) {
    return db.get(schema.getColumnSlotsByFinalizedStateRoot(), stateRoot);
  }

  @Override
  public Optional<SlotAndBlockRoot> getSlotAndBlockRootForFinalizedStateRoot(
      final Bytes32 stateRoot) {
    Optional<UInt64> maybeSlot = db.get(schema.getColumnSlotsByFinalizedStateRoot(), stateRoot);
    return maybeSlot.flatMap(
        slot ->
            getFinalizedBlockAtSlot(slot)
                .map(block -> new SlotAndBlockRoot(slot, block.getRoot())));
  }

  @Override
  public Optional<? extends SignedBeaconBlock> getNonCanonicalBlock(final Bytes32 root) {
    return db.get(schema.getColumnNonCanonicalBlocksByRoot(), root);
  }

  @Override
  public void ingest(
      final KvStoreFinalizedDao finalizedDao, final int batchSize, final Consumer<String> logger) {
    Preconditions.checkArgument(batchSize > 1, "Batch size must be greater than 1 element");
    Preconditions.checkArgument(finalizedDao instanceof V4FinalizedKvStoreDao);

    final Map<String, KvStoreVariable<?>> newVariables = schema.getVariableMap();
    if (newVariables.size() > 0) {
      final Map<String, KvStoreVariable<?>> oldVariables =
          ((V4FinalizedKvStoreDao) finalizedDao).schema.getVariableMap();
      try (V4FinalizedUpdater updater = new V4FinalizedUpdater(db, schema, UInt64.ONE)) {
        for (String key : newVariables.keySet()) {
          logger.accept(String.format("Copy variable %s", key));
          finalizedDao
              .getRawVariable(oldVariables.get(key))
              .ifPresent(value -> updater.transaction.putRaw(newVariables.get(key), value));
        }
        updater.commit();
      }
    }
    final Map<String, KvStoreColumn<?, ?>> newColumns = schema.getColumnMap();
    if (newColumns.size() > 0) {
      final Map<String, KvStoreColumn<?, ?>> oldColumns =
          ((V4FinalizedKvStoreDao) finalizedDao).schema.getColumnMap();
      for (String key : newColumns.keySet()) {
        logger.accept(String.format("Copy column %s", key));
        final List<ColumnEntry<Bytes, Bytes>> buffer = new ArrayList<>();
        final AtomicInteger counter = new AtomicInteger(0);
        try (final Stream<ColumnEntry<Bytes, Bytes>> oldEntryStream =
            finalizedDao.streamRawColumn(oldColumns.get(key))) {
          oldEntryStream.forEach(
              entry -> {
                buffer.add(entry);
                if (buffer.size() >= batchSize) {
                  pushColumnEntryBatch(newColumns.get(key), buffer);
                  buffer.clear();
                }
                if (counter.incrementAndGet() % 100_000 == 0) {
                  logger.accept(String.format(" -- %,d...", counter.get()));
                }
              });
        }
        if (buffer.size() > 0) {
          pushColumnEntryBatch(newColumns.get(key), buffer);
          buffer.clear();
        }
        logger.accept(String.format(" => Inserted %,d rows", counter.get()));
      }
    }
  }

  private <K, V> void pushColumnEntryBatch(
      final KvStoreColumn<K, V> column, final List<ColumnEntry<Bytes, Bytes>> buffer) {
    try (V4FinalizedUpdater updater = new V4FinalizedUpdater(db, schema, UInt64.ONE)) {
      buffer.forEach(entry -> updater.transaction.putRaw(column, entry.getKey(), entry.getValue()));
      updater.commit();
    }
  }

  @Override
  public <T> Optional<Bytes> getRawVariable(final KvStoreVariable<T> var) {
    return db.getRaw(var);
  }

  @Override
  @MustBeClosed
  public <K, V> Stream<ColumnEntry<Bytes, Bytes>> streamRawColumn(
      final KvStoreColumn<K, V> kvStoreColumn) {
    return db.streamRaw(kvStoreColumn);
  }

  @Override
  public Optional<SignedBeaconBlock> getFinalizedBlock(final Bytes32 root) {
    return db.get(schema.getColumnSlotsByFinalizedRoot(), root)
        .flatMap(this::getFinalizedBlockAtSlot);
  }

  @Override
  @MustBeClosed
  public FinalizedUpdater finalizedUpdater() {
    return new V4FinalizedKvStoreDao.V4FinalizedUpdater(db, schema, stateStorageFrequency);
  }

  private static class V4FinalizedUpdater implements FinalizedUpdater {
    private final KvStoreTransaction transaction;
    private final KvStoreAccessor db;
    private final SchemaFinalized schema;
    private final UInt64 stateStorageFrequency;
    private Optional<UInt64> lastStateStoredSlot = Optional.empty();
    private boolean loadedLastStoreState = false;

    V4FinalizedUpdater(
        final KvStoreAccessor db,
        final SchemaFinalized schema,
        final UInt64 stateStorageFrequency) {
      this.transaction = db.startTransaction();
      this.db = db;
      this.schema = schema;
      this.stateStorageFrequency = stateStorageFrequency;
    }

    @Override
    public void addFinalizedBlock(final SignedBeaconBlock block) {
      transaction.put(schema.getColumnSlotsByFinalizedRoot(), block.getRoot(), block.getSlot());
      transaction.put(schema.getColumnFinalizedBlocksBySlot(), block.getSlot(), block);
    }

    @Override
    public void addNonCanonicalBlock(final SignedBeaconBlock block) {
      transaction.put(schema.getColumnNonCanonicalBlocksByRoot(), block.getRoot(), block);
    }

    @Override
    public void addNonCanonicalRootAtSlot(final UInt64 slot, final Set<Bytes32> blockRoots) {
      Optional<Set<Bytes32>> maybeRoots = db.get(schema.getColumnNonCanonicalRootsBySlot(), slot);
      final Set<Bytes32> roots = maybeRoots.orElse(new HashSet<>());
      if (roots.addAll(blockRoots)) {
        transaction.put(schema.getColumnNonCanonicalRootsBySlot(), slot, roots);
      }
    }

    @Override
    public void addFinalizedState(final Bytes32 blockRoot, final BeaconState state) {
      if (!loadedLastStoreState) {
        lastStateStoredSlot = db.getLastKey(schema.getColumnFinalizedStatesBySlot());
        loadedLastStoreState = true;
      }
      if (lastStateStoredSlot.isPresent()) {
        UInt64 nextStorageSlot = lastStateStoredSlot.get().plus(stateStorageFrequency);
        if (state.getSlot().compareTo(nextStorageSlot) >= 0) {
          addFinalizedState(state);
        }
      } else {
        addFinalizedState(state);
      }
    }

    @Override
    public void addFinalizedStateRoot(final Bytes32 stateRoot, final UInt64 slot) {
      transaction.put(schema.getColumnSlotsByFinalizedStateRoot(), stateRoot, slot);
    }

    private void addFinalizedState(final BeaconState state) {
      transaction.put(schema.getColumnFinalizedStatesBySlot(), state.getSlot(), state);
      lastStateStoredSlot = Optional.of(state.getSlot());
    }

    @Override
    public void commit() {
      // Commit db updates
      transaction.commit();
      close();
    }

    @Override
    public void cancel() {
      transaction.rollback();
      close();
    }

    @Override
    public void close() {
      transaction.close();
    }
  }
}
