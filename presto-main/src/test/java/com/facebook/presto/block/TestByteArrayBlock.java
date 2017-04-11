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
package com.facebook.presto.block;

import com.facebook.presto.spi.block.BlockBuilder;
import com.facebook.presto.spi.block.BlockBuilderStatus;
import com.facebook.presto.spi.block.ByteArrayBlockBuilder;
import com.google.common.primitives.Ints;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import org.testng.annotations.Test;

public class TestByteArrayBlock
        extends AbstractTestBlock
{
    @Test
    public void test()
    {
        Slice[] expectedValues = createTestValue(17);
        assertFixedWithValues(expectedValues);
        assertFixedWithValues((Slice[]) alternatingNullValues(expectedValues));
    }

    @Test
    public void testCopyPositions()
            throws Exception
    {
        Slice[] expectedValues = (Slice[]) alternatingNullValues(createTestValue(17));
        BlockBuilder blockBuilder = createBlockBuilderWithValues(expectedValues);
        assertBlockFilteredPositions(expectedValues, blockBuilder.build(), Ints.asList(0, 2, 4, 6, 7, 9, 10, 16));
    }

    private void assertFixedWithValues(Slice[] expectedValues)
    {
        BlockBuilder blockBuilder = createBlockBuilderWithValues(expectedValues);
        assertBlock(blockBuilder, expectedValues);
        assertBlock(blockBuilder.build(), expectedValues);
    }

    private static BlockBuilder createBlockBuilderWithValues(Slice[] expectedValues)
    {
        ByteArrayBlockBuilder blockBuilder = new ByteArrayBlockBuilder(new BlockBuilderStatus(), expectedValues.length);
        for (Slice expectedValue : expectedValues) {
            if (expectedValue == null) {
                blockBuilder.appendNull();
            }
            else {
                blockBuilder.writeByte(expectedValue.getByte(0)).closeEntry();
            }
        }
        return blockBuilder;
    }

    private static Slice[] createTestValue(int positionCount)
    {
        Slice[] expectedValues = new Slice[positionCount];
        for (int position = 0; position < positionCount; position++) {
            expectedValues[position] = Slices.wrappedBuffer((byte) position);
        }
        return expectedValues;
    }

    @Override
    protected boolean isShortAccessSupported()
    {
        return false;
    }

    @Override
    protected boolean isIntAccessSupported()
    {
        return false;
    }

    @Override
    protected boolean isLongAccessSupported()
    {
        return false;
    }

    @Override
    protected boolean isSliceAccessSupported()
    {
        return false;
    }
}
