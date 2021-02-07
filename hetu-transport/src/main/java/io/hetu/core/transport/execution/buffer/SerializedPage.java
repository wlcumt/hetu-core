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
package io.hetu.core.transport.execution.buffer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.airlift.slice.Slice;
import io.airlift.slice.Slices;
import io.prestosql.spi.block.Block;
import org.openjdk.jol.info.ClassLayout;

import java.util.Properties;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static io.hetu.core.transport.execution.buffer.PageCodecMarker.COMPRESSED;
import static io.hetu.core.transport.execution.buffer.PageCodecMarker.ENCRYPTED;
import static io.hetu.core.transport.execution.buffer.PageCodecMarker.MarkerSet.fromByteValue;
import static java.util.Objects.requireNonNull;

public class SerializedPage
{
    private static final int INSTANCE_SIZE = ClassLayout.parseClass(SerializedPage.class).instanceSize();

    private Slice slice;
    private Block[] blocks;
    private final int positionCount;
    private final int uncompressedSizeInBytes;
    private final byte pageCodecMarkers;
    private Properties pageMetadata = new Properties();

    @JsonCreator
    public SerializedPage(
            @JsonProperty("sliceArray") byte[] sliceArray,
            @JsonProperty("pageCodecMarkers") byte pageCodecMarkers,
            @JsonProperty("positionCount") int positionCount,
            @JsonProperty("uncompressedSizeInBytes") int uncompressedSizeInBytes,
            @JsonProperty("pageMetadata") Properties pageMetadata)
    {
        this(
                Slices.wrappedBuffer(sliceArray),
                fromByteValue(pageCodecMarkers),
                positionCount,
                uncompressedSizeInBytes,
                pageMetadata);
    }

    public SerializedPage(byte[] sliceArray, byte pageCodecMarkers, int positionCount, int uncompressedSizeInBytes)
    {
        this(
                Slices.wrappedBuffer(sliceArray),
                fromByteValue(pageCodecMarkers),
                positionCount,
                uncompressedSizeInBytes);
    }

    public SerializedPage(Slice slice, PageCodecMarker.MarkerSet markers, int positionCount, int uncompressedSizeInBytes, Properties pageMetadata)
    {
        this.slice = requireNonNull(slice, "slice is null");
        this.positionCount = positionCount;
        checkArgument(uncompressedSizeInBytes >= 0, "uncompressedSizeInBytes is negative");
        this.uncompressedSizeInBytes = uncompressedSizeInBytes;
        this.pageCodecMarkers = requireNonNull(markers, "markers is null").byteValue();
        this.pageMetadata = pageMetadata == null ? new Properties() : pageMetadata;
        //  Encrypted pages may include arbitrary overhead from ciphers, sanity checks skipped
        if (!markers.contains(ENCRYPTED)) {
            if (markers.contains(COMPRESSED)) {
                checkArgument(uncompressedSizeInBytes > slice.length(), "compressed size must be smaller than uncompressed size when compressed");
            }
            else {
                checkArgument(uncompressedSizeInBytes == slice.length(), "uncompressed size must be equal to slice length when uncompressed");
            }
        }
    }

    public SerializedPage(Slice slice, PageCodecMarker.MarkerSet markers, int positionCount, int uncompressedSizeInBytes)
    {
        this(slice, markers, positionCount, uncompressedSizeInBytes, null);
    }

    public SerializedPage(Block[] blocks, PageCodecMarker.MarkerSet markers, int positionCount, int uncompressedSizeInBytes, Properties pageMetadata)
    {
        this.blocks = requireNonNull(blocks, "blocks is null");
        this.positionCount = positionCount;
        checkArgument(uncompressedSizeInBytes >= 0, "uncompressedSizeInBytes is negative");
        this.uncompressedSizeInBytes = uncompressedSizeInBytes;
        this.pageCodecMarkers = requireNonNull(markers, "markers is null").byteValue();
        this.pageMetadata = pageMetadata == null ? new Properties() : pageMetadata;
    }

    public int getSizeInBytes()
    {
        if (slice != null) {
            return slice.length();
        }
        else {
            int size = 0;
            for (Block block : blocks) {
                size += block.getSizeInBytes();
            }
            return size;
        }
    }

    @JsonProperty
    public int getUncompressedSizeInBytes()
    {
        return uncompressedSizeInBytes;
    }

    public long getRetainedSizeInBytes()
    {
        if (slice != null) {
            return INSTANCE_SIZE + slice.getRetainedSize();
        }
        else {
            int size = 0;
            for (Block block : blocks) {
                size += block.getRetainedSizeInBytes();
            }
            return size;
        }
    }

    @JsonProperty
    public int getPositionCount()
    {
        return positionCount;
    }

    @JsonProperty
    public byte[] getSliceArray()
    {
        if (slice != null) {
            return slice.getBytes();
        }
        else {
            return null;
        }
    }

    public Slice getSlice()
    {
        return slice;
    }

    @JsonProperty
    public byte getPageCodecMarkers()
    {
        return pageCodecMarkers;
    }

    public boolean isCompressed()
    {
        return COMPRESSED.isSet(pageCodecMarkers);
    }

    public boolean isEncrypted()
    {
        return ENCRYPTED.isSet(pageCodecMarkers);
    }

    public boolean isOffHeap()
    {
        return slice == null && blocks != null && blocks.length != 0;
    }

    public Block[] getBlocks()
    {
        return blocks;
    }

    @JsonProperty
    public Properties getPageMetadata()
    {
        return pageMetadata;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("positionCount", positionCount)
                .add("pageCodecMarkers", PageCodecMarker.toSummaryString(pageCodecMarkers))
                .add("sizeInBytes", slice.length())
                .add("uncompressedSizeInBytes", uncompressedSizeInBytes)
                .add("pageMetadata", pageMetadata)
                .toString();
    }
}
