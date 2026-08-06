package me.cortex.nvidium.util;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import me.cortex.nvidium.gl.GlFence;
import me.cortex.nvidium.gl.RenderDevice;
import me.cortex.nvidium.gl.buffers.Buffer;
import me.cortex.nvidium.gl.buffers.PersistentClientMappedBuffer;

import java.util.ArrayDeque;
import java.util.Deque;

import static me.cortex.nvidium.util.SegmentedManager.SIZE_LIMIT;
import static org.lwjgl.opengl.ARBDirectStateAccess.glCopyNamedBufferSubData;
import static org.lwjgl.opengl.ARBDirectStateAccess.glFlushMappedNamedBufferRange;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL42C.GL_BUFFER_UPDATE_BARRIER_BIT;
import static org.lwjgl.opengl.GL42C.glMemoryBarrier;
import static org.lwjgl.opengl.GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT;

public class UploadingBufferStream {
    private static final long MEBIBYTE = 1024L * 1024L;

    private final SegmentedManager allocationArena = new SegmentedManager();
    private final PersistentClientMappedBuffer uploadBuffer;
    private final long capacityBytes;

    private final Deque<UploadFrame> frames = new ArrayDeque<>();
    private final LongArrayList thisFrameAllocations = new LongArrayList();
    private final Deque<UploadData> uploadList = new ArrayDeque<>();
    private final LongArrayList flushList = new LongArrayList();

    private long forcedWaitCount;
    private long currentAllocation = -1;
    private long currentOffset;

    public UploadingBufferStream(RenderDevice device, long size) {
        if (size <= 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Upload buffer size must be between 1 byte and 2 GiB");
        }

        this.capacityBytes = size;
        this.allocationArena.setLimit(size);
        this.uploadBuffer = device.createClientMappedBuffer(size);
        TickableManager.register(this);
    }

    public long upload(Buffer buffer, long destinationOffset, long size) {
        if (size > Integer.MAX_VALUE || size <= 0) {
            throw new IllegalArgumentException("Invalid upload size: " + size);
        }
        if (size > this.capacityBytes) {
            throw new IllegalArgumentException("A single upload exceeds the staging buffer capacity");
        }
        if (destinationOffset < 0 || destinationOffset + size > buffer.getSize()) {
            throw new IllegalStateException("Upload destination is outside the target buffer");
        }

        long address;
        if (this.currentAllocation == -1 || !this.allocationArena.expand(this.currentAllocation, (int) size)) {
            this.currentAllocation = this.allocationArena.alloc((int) size);

            if (this.currentAllocation == SIZE_LIMIT) {
                this.forcedWaitCount++;
                this.commit();

                int attempts = 10;
                while (--attempts != 0 && this.currentAllocation == SIZE_LIMIT) {
                    glFinish();
                    this.tick();
                    this.currentAllocation = this.allocationArena.alloc((int) size);
                }

                if (this.currentAllocation == SIZE_LIMIT) {
                    throw new IllegalStateException("Could not allocate an upload segment after a forced flush");
                }
            }

            this.flushList.add(this.currentAllocation);
            this.currentOffset = size;
            address = this.currentAllocation;
        } else {
            address = this.currentAllocation + this.currentOffset;
            this.currentOffset += size;
        }

        if (address + size > this.uploadBuffer.size) {
            throw new IllegalStateException("Upload allocation exceeds staging buffer capacity");
        }

        this.uploadList.add(new UploadData(buffer, address, destinationOffset, size));
        return this.uploadBuffer.addr + address;
    }

    public void commit() {
        for (long allocation : this.flushList) {
            glFlushMappedNamedBufferRange(
                    this.uploadBuffer.getId(),
                    allocation,
                    this.allocationArena.getSize(allocation)
            );
            this.thisFrameAllocations.add(allocation);
        }
        this.flushList.clear();

        glMemoryBarrier(GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);

        for (UploadData entry : this.uploadList) {
            glCopyNamedBufferSubData(
                    this.uploadBuffer.getId(),
                    entry.target.getId(),
                    entry.uploadOffset,
                    entry.targetOffset,
                    entry.size
            );
        }
        this.uploadList.clear();

        glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT);
        this.currentAllocation = -1;
        this.currentOffset = 0;
    }

    public void tick() {
        this.commit();

        if (!this.thisFrameAllocations.isEmpty()) {
            this.frames.add(new UploadFrame(new GlFence(), new LongArrayList(this.thisFrameAllocations)));
            this.thisFrameAllocations.clear();
        }

        while (!this.frames.isEmpty()) {
            if (!this.frames.peek().fence.signaled()) {
                break;
            }

            UploadFrame frame = this.frames.pop();
            frame.allocations.forEach(this.allocationArena::free);
            frame.fence.free();
        }
    }

    public int getCapacityMiB() {
        return (int) (this.capacityBytes / MEBIBYTE);
    }

    public long getForcedWaitCount() {
        return this.forcedWaitCount;
    }

    public void delete() {
        TickableManager.remove(this);
        this.uploadBuffer.delete();
        this.frames.forEach(frame -> frame.fence.free());
    }

    private record UploadFrame(GlFence fence, LongArrayList allocations) {
    }

    private record UploadData(Buffer target, long uploadOffset, long targetOffset, long size) {
    }
}
