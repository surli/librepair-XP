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

import com.facebook.presto.spi.function.OperatorType;
import com.facebook.presto.spi.type.Type;
import com.facebook.presto.spi.type.TypeManager;
import com.facebook.presto.spi.type.TypeSerde;
import io.airlift.slice.SliceInput;
import io.airlift.slice.SliceOutput;

import java.lang.invoke.MethodHandle;

import static com.facebook.presto.spi.block.AbstractMapBlock.HASH_MULTIPLIER;
import static com.facebook.presto.spi.block.MethodHandleUtil.compose;
import static com.facebook.presto.spi.block.MethodHandleUtil.nativeValueGetter;
import static io.airlift.slice.Slices.wrappedIntArray;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public class MapElementBlockEncoding
        implements BlockEncoding
{
    public static final BlockEncodingFactory<MapElementBlockEncoding> FACTORY = new MapElementBlockEncodingFactory();
    private static final String NAME = "MAP_ELEMENT";

    private final Type keyType;
    private final MethodHandle keyNativeHashCode;
    private final MethodHandle keyBlockNativeEquals;
    private final BlockEncoding keyBlockEncoding;
    private final BlockEncoding valueBlockEncoding;

    public MapElementBlockEncoding(Type keyType, MethodHandle keyNativeHashCode, MethodHandle keyBlockNativeEquals, BlockEncoding keyBlockEncoding, BlockEncoding valueBlockEncoding)
    {
        this.keyType = requireNonNull(keyType, "keyType is null");
        this.keyNativeHashCode = requireNonNull(keyNativeHashCode, "keyNativeHashCode is null");
        this.keyBlockNativeEquals = requireNonNull(keyBlockNativeEquals, "keyBlockNativeEquals");
        this.keyBlockEncoding = requireNonNull(keyBlockEncoding, "keyBlockEncoding is null");
        this.valueBlockEncoding = requireNonNull(valueBlockEncoding, "valueBlockEncoding is null");
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public void writeBlock(SliceOutput sliceOutput, Block block)
    {
        MapElementBlock mapElementBlock = (MapElementBlock) block;
        int offset = mapElementBlock.getOffset();
        int positionCount = mapElementBlock.getPositionCount();
        keyBlockEncoding.writeBlock(sliceOutput, mapElementBlock.getKeyBlock().getRegion(offset / 2, positionCount / 2));
        valueBlockEncoding.writeBlock(sliceOutput, mapElementBlock.getValueBlock().getRegion(offset / 2, positionCount / 2));
        int[] hashTable = mapElementBlock.getHashTable();
        sliceOutput.appendInt(positionCount / 2 * HASH_MULTIPLIER);
        sliceOutput.writeBytes(wrappedIntArray(hashTable, offset / 2 * HASH_MULTIPLIER, positionCount / 2 * HASH_MULTIPLIER));
    }

    @Override
    public Block readBlock(SliceInput sliceInput)
    {
        Block keyBlock = keyBlockEncoding.readBlock(sliceInput);
        Block valueBlock = valueBlockEncoding.readBlock(sliceInput);

        int[] hashTable = new int[sliceInput.readInt()];
        sliceInput.readBytes(wrappedIntArray(hashTable));

        if (keyBlock.getPositionCount() != valueBlock.getPositionCount() || keyBlock.getPositionCount() * HASH_MULTIPLIER != hashTable.length) {
            throw new IllegalArgumentException(
                    format("Deserialized MapElementBlock violates invariants: key %d, value %d, hash %d", keyBlock.getPositionCount(), valueBlock.getPositionCount(), hashTable.length));
        }

        return new MapElementBlock(0, keyBlock.getPositionCount() * 2, keyBlock, valueBlock, hashTable, keyType, keyNativeHashCode, keyBlockNativeEquals);
    }

    @Override
    public BlockEncodingFactory getFactory()
    {
        return FACTORY;
    }

    public static class MapElementBlockEncodingFactory
            implements BlockEncodingFactory<MapElementBlockEncoding>
    {
        @Override
        public String getName()
        {
            return NAME;
        }

        @Override
        public MapElementBlockEncoding readEncoding(TypeManager typeManager, BlockEncodingSerde serde, SliceInput input)
        {
            Type keyType = TypeSerde.readType(typeManager, input);
            MethodHandle keyNativeHashCode = typeManager.resolveOperator(OperatorType.HASH_CODE, singletonList(keyType));
            MethodHandle keyNativeEquals = typeManager.resolveOperator(OperatorType.EQUAL, asList(keyType, keyType));
            MethodHandle keyBlockNativeEquals = compose(keyNativeEquals, nativeValueGetter(keyType));

            BlockEncoding keyBlockEncoding = serde.readBlockEncoding(input);
            BlockEncoding valueBlockEncoding = serde.readBlockEncoding(input);
            return new MapElementBlockEncoding(keyType, keyNativeHashCode, keyBlockNativeEquals, keyBlockEncoding, valueBlockEncoding);
        }

        @Override
        public void writeEncoding(BlockEncodingSerde serde, SliceOutput output, MapElementBlockEncoding blockEncoding)
        {
            TypeSerde.writeType(output, blockEncoding.keyType);
            serde.writeBlockEncoding(output, blockEncoding.keyBlockEncoding);
            serde.writeBlockEncoding(output, blockEncoding.valueBlockEncoding);
        }
    }
}
