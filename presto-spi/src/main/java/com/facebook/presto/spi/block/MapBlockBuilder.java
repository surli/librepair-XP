/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.presto.spi.block;

import com.facebook.presto.spi.type.Type;
import org.openjdk.jol.info.ClassLayout;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;

import static com.facebook.presto.spi.block.BlockUtil.calculateBlockResetSize;
import static com.facebook.presto.spi.block.BlockUtil.intSaturatedCast;
import static io.airlift.slice.SizeOf.sizeOf;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class MapBlockBuilder
        extends AbstractMapBlock
        implements BlockBuilder
{
    private static final int INSTANCE_SIZE = ClassLayout.parseClass(MapBlockBuilder.class).instanceSize() + BlockBuilderStatus.INSTANCE_SIZE;

    private final MethodHandle keyBlockHashCode;

    private BlockBuilderStatus blockBuilderStatus;

    private int positionCount;
    private int[] offsets;
    private boolean[] mapIsNull;
    private final BlockBuilder keyBlockBuilder;
    private final BlockBuilder valueBlockBuilder;
    private int[] hashTables;

    private int currentEntrySize;

    public MapBlockBuilder(
            Type keyType,
            Type valueType,
            MethodHandle keyBlockNativeEquals, MethodHandle keyNativeHashCode,
            MethodHandle keyBlockHashCode,
            BlockBuilderStatus blockBuilderStatus,
            int expectedEntries)
    {
        this(
                keyType,
                keyBlockNativeEquals, keyNativeHashCode,
                keyBlockHashCode,
                blockBuilderStatus,
                keyType.createBlockBuilder(blockBuilderStatus, expectedEntries),
                valueType.createBlockBuilder(blockBuilderStatus, expectedEntries),
                new int[expectedEntries + 1],
                new boolean[expectedEntries],
                newNegativeOneFilledArray(expectedEntries * HASH_MULTIPLIER));
    }

    private MapBlockBuilder(
            Type keyType,
            MethodHandle keyBlockNativeEquals, MethodHandle keyNativeHashCode,
            MethodHandle keyBlockHashCode,
            BlockBuilderStatus blockBuilderStatus,
            BlockBuilder keyBlockBuilder,
            BlockBuilder valueBlockBuilder,
            int[] offsets,
            boolean[] mapIsNull,
            int[] hashTables)
    {
        super(keyType, keyNativeHashCode, keyBlockNativeEquals);

        this.keyBlockHashCode = requireNonNull(keyBlockHashCode, "keyBlockHashCode is null");
        this.blockBuilderStatus = requireNonNull(blockBuilderStatus, "blockBuilderStatus is null");

        this.positionCount = 0;
        this.offsets = requireNonNull(offsets, "offsets is null");
        this.mapIsNull = requireNonNull(mapIsNull, "mapIsNull is null");
        this.keyBlockBuilder = requireNonNull(keyBlockBuilder, "keyBlockBuilder is null");
        this.valueBlockBuilder = requireNonNull(valueBlockBuilder, "valueBlockBuilder is null");
        this.hashTables = requireNonNull(hashTables, "hashTables is null");
    }

    @Override
    protected Block getKeys()
    {
        return keyBlockBuilder;
    }

    @Override
    protected Block getValues()
    {
        return valueBlockBuilder;
    }

    @Override
    protected int[] getHashTables()
    {
        return hashTables;
    }

    @Override
    protected int[] getOffsets()
    {
        return offsets;
    }

    @Override
    protected int getOffsetBase()
    {
        return 0;
    }

    @Override
    protected boolean[] getMapIsNull()
    {
        return mapIsNull;
    }

    @Override
    public int getPositionCount()
    {
        return positionCount;
    }

    @Override
    public int getSizeInBytes()
    {
        return keyBlockBuilder.getSizeInBytes() + valueBlockBuilder.getSizeInBytes() +
                (Integer.BYTES + Byte.BYTES) * positionCount +
                Integer.BYTES * HASH_MULTIPLIER * keyBlockBuilder.getPositionCount();
    }

    @Override
    public int getRetainedSizeInBytes()
    {
        return intSaturatedCast(INSTANCE_SIZE + keyBlockBuilder.getRetainedSizeInBytes() + valueBlockBuilder.getRetainedSizeInBytes() + sizeOf(offsets) + sizeOf(mapIsNull) + sizeOf(hashTables));
    }

    @Override
    public MapElementBlockWriter beginBlockEntry()
    {
        if (currentEntrySize != 0) {
            throw new IllegalStateException("Expected current entry size to be exactly 0 but was " + currentEntrySize);
        }
        currentEntrySize++;
        return new MapElementBlockWriter(keyBlockBuilder.getPositionCount() * 2, keyBlockBuilder, valueBlockBuilder);
    }

    @Override
    public BlockBuilder closeEntry()
    {
        if (currentEntrySize != 1) {
            throw new IllegalStateException("Expected entry size to be exactly 1 but was " + currentEntrySize);
        }

        entryAdded(false);
        currentEntrySize = 0;

        int previousAggregatedEntryCount = offsets[positionCount - 1];
        int aggregatedEntryCount = offsets[positionCount];
        int entryCount = aggregatedEntryCount - previousAggregatedEntryCount;
        if (hashTables.length < aggregatedEntryCount * HASH_MULTIPLIER) {
            int newSize = BlockUtil.calculateNewArraySize(aggregatedEntryCount * HASH_MULTIPLIER);
            int oldSize = hashTables.length;
            hashTables = Arrays.copyOf(hashTables, newSize);
            Arrays.fill(hashTables, oldSize, hashTables.length, -1);
        }
        buildHashTable(keyBlockBuilder, previousAggregatedEntryCount, entryCount, keyBlockHashCode, hashTables, previousAggregatedEntryCount * HASH_MULTIPLIER, entryCount * HASH_MULTIPLIER);
        blockBuilderStatus.addBytes(entryCount * HASH_MULTIPLIER * Integer.BYTES);

        return this;
    }

    @Override
    public BlockBuilder appendNull()
    {
        if (currentEntrySize > 0) {
            throw new IllegalStateException("Current entry must be closed before a null can be written");
        }

        entryAdded(true);
        return this;
    }

    private void entryAdded(boolean isNull)
    {
        if (keyBlockBuilder.getPositionCount() != valueBlockBuilder.getPositionCount()) {
            throw new IllegalStateException(format("keyBlock and valueBlock has different size: %s %s", keyBlockBuilder.getPositionCount(), valueBlockBuilder.getPositionCount()));
        }
        if (mapIsNull.length <= positionCount) {
            int newSize = BlockUtil.calculateNewArraySize(mapIsNull.length);
            mapIsNull = Arrays.copyOf(mapIsNull, newSize);
            offsets = Arrays.copyOf(offsets, newSize + 1);
        }
        offsets[positionCount + 1] = keyBlockBuilder.getPositionCount();
        mapIsNull[positionCount] = isNull;
        positionCount++;

        blockBuilderStatus.addBytes(Integer.BYTES + Byte.BYTES);
    }

    @Override
    public Block build()
    {
        return new MapBlock(
                0,
                positionCount,
                mapIsNull,
                offsets,
                keyBlockBuilder.build(),
                valueBlockBuilder.build(),
                Arrays.copyOf(hashTables, offsets[positionCount] * HASH_MULTIPLIER),
                keyType,
                keyBlockNativeEquals, keyNativeHashCode
        );
    }

    @Override
    public void reset(BlockBuilderStatus blockBuilderStatus)
    {
        this.blockBuilderStatus = requireNonNull(blockBuilderStatus, "blockBuilderStatus is null");

        int newSize = calculateBlockResetSize(getPositionCount());
        int newHashTableSize = calculateBlockResetSize(offsets[positionCount + 1]);
        mapIsNull = new boolean[newSize];
        offsets = new int[newSize + 1];
        keyBlockBuilder.reset(blockBuilderStatus);
        valueBlockBuilder.reset(blockBuilderStatus);
        hashTables = new int[newHashTableSize];

        currentEntrySize = 0;
        positionCount = 0;
    }

    @Override
    public String toString()
    {
        return "MapBlockBuilder{" +
                "positionCount=" + getPositionCount() +
                '}';
    }

    @Override
    public BlockBuilder writeObject(Object value)
    {
        if (currentEntrySize != 0) {
            throw new IllegalStateException("Expected current entry size to be exactly 0 but was " + currentEntrySize);
        }
        currentEntrySize++;

        Block block = (Block) value;
        int blockPositionCount = block.getPositionCount();
        if (blockPositionCount % 2 != 0) {
            throw new IllegalArgumentException(format("block position count is not even: %s", blockPositionCount));
        }
        for (int i = 0; i < blockPositionCount; i += 2) {
            if (block.isNull(i)) {
                throw new IllegalArgumentException("Map keys must not be null");
            }
            else {
                block.writePositionTo(i, keyBlockBuilder);
                keyBlockBuilder.closeEntry();
            }
            if (block.isNull(i + 1)) {
                valueBlockBuilder.appendNull();
            }
            else {
                block.writePositionTo(i + 1, valueBlockBuilder);
                valueBlockBuilder.closeEntry();
            }
        }
        return this;
    }

    @Override
    public BlockBuilder newBlockBuilderLike(BlockBuilderStatus blockBuilderStatus)
    {
        int newSize = calculateBlockResetSize(getPositionCount());
        return new MapBlockBuilder(
                keyType,
                keyBlockNativeEquals, keyNativeHashCode,
                keyBlockHashCode,
                blockBuilderStatus,
                keyBlockBuilder.newBlockBuilderLike(blockBuilderStatus),
                valueBlockBuilder.newBlockBuilderLike(blockBuilderStatus),
                new int[newSize + 1],
                new boolean[newSize],
                newNegativeOneFilledArray(newSize * HASH_MULTIPLIER));
    }

    private static int[] newNegativeOneFilledArray(int size)
    {
        int[] hashTable = new int[size];
        Arrays.fill(hashTable, -1);
        return hashTable;
    }

    private static void buildHashTable(Block keyBlock, int keyOffset, int keyCount, MethodHandle keyBlockHashCode, int[] outputHashTable, int hashTableOffset, int hashTableSize)
    {
        // This method assumes that keyBlock has no duplicated entries (in the specified range)
        for (int i = 0; i < keyCount; i++) {
            if (keyBlock.isNull(keyOffset + i)) {
                throw new IllegalArgumentException("map keys cannot be null");
            }

            long hashCode;
            try {
                hashCode = (long) keyBlockHashCode.invokeExact(keyBlock, keyOffset + i);
            }
            catch (Throwable throwable) {
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                }
                throw new RuntimeException(throwable);
            }

            int hash = (int) Math.floorMod(hashCode, hashTableSize);
            while (true) {
                if (outputHashTable[hashTableOffset + hash] == -1) {
                    outputHashTable[hashTableOffset + hash] = i;
                    break;
                }
                hash++;
                if (hash == hashTableSize) {
                    hash = 0;
                }
            }
        }
    }
}
