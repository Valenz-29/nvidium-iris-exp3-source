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
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL42C.GL_BUFFER_UPDATE_BARRIER_BIT;
import static org.lwjgl.opengl.GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT;

public class UploadingBufferStream {
    private final SegmentedManager allocationArena = new SegmentedManager();
    private final PersistentClientMappedBuffer uploadBuffer;
    private final long capacityBytes;

    private final Deque<UploadFrame> frames = new ArrayDeque<>();
    private final LongArrayList thisFrameAllocations = new LongArrayList();
    private final Deque<UploadData> uploadList = new ArrayDeque<>();
    private final LongArrayList flushList = new LongArrayList();

    private long forcedWaitCount;

    public UploadingBufferStream(RenderDevice device, long size) {
        this.capacityBytes = size;
        this.allocationArena.setLimit(size);
        this.uploadBuffer = device.createClientMappedBuffer(size);
        TickableManager.register(this);
    }

    private long caddr = -1;
    private long offset = 0;

    public long upload(Buffer buffer, long destOffset, long size) {
        if (size > Integer.MAX_VALUE || size == 0 || size < 0) {
            throw new IllegalArgumentException();
        }
        if (size > this.capacityBytes) {
            throw new IllegalArgumentException("Single upload exceeds staging buffer capacity");
        }
        if (destOffset < 0) {
            throw new IllegalStateException();
        }
        if (destOffset + size > buffer.getSize()) {
            throw new IllegalStateException();
        }

        long addr;
        if (this.caddr == -1 || !this.allocationArena.expand(this.caddr, (int) size)) {
            this.caddr = this.allocationArena.alloc((int) size);
            // If the upload stream is full, flush it and empty it.
            if (this.caddr == SIZE_LIMIT) {
                this.forcedWaitCount++;
                this.commit();
                int attempts = 10;
                while (--attempts != 0 && this.caddr == SIZE_LIMIT) {
                    glFinish();
                    this.tick();
                    this.caddr = this.allocationArena.alloc((int) size);
                }
                if (this.caddr == SIZE_LIMIT) {
                    throw new IllegalStateException("Could not allocate memory segment big enough for upload even after force flush");
                }
            }
            this.flushList.add(this.caddr);
            this.offset = size;
            addr = this.caddr;
        } else {
            addr = this.caddr + this.offset;
            this.offset += size;
        }

        if (addr + size > this.uploadBuffer.size) {
            throw new IllegalStateException();
        }

        this.uploadList.add(new UploadData(buffer, addr, destOffset, size));
        return this.uploadBuffer.addr + addr;
    }

    public void commit() {
        for (long alloc : flushList) {
            glFlushMappedNamedBufferRange(this.uploadBuffer.getId(), alloc, this.allocationArena.getSize(alloc));
            this.thisFrameAllocations.add(alloc);
        }
        this.flushList.clear();

        glMemoryBarrier(GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);

        for (var entry : this.uploadList) {
            glCopyNamedBufferSubData(this.uploadBuffer.getId(), entry.target.getId(), entry.uploadOffset, entry.targetOffset, entry.size);
        }
        this.uploadList.clear();

        glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT);

        this.caddr = -1;
        this.offset = 0;
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
            var frame = this.frames.pop();
            frame.allocations.forEach(allocationArena::free);
            frame.fence.free();
        }
    }

    public int getCapacityMiB() {
        return (int) (this.capacityBytes / (1024L * 1024L));
    }

    public long getForcedWaitCount() {
        return this.forcedWaitCount;
    }

    public void delete() {
        TickableManager.remove(this);
        this.uploadBuffer.delete();
        this.frames.forEach(frame -> frame.fence.free());
    }

    private record UploadFrame(GlFence fence, LongArrayList allocations) {}
    private record UploadData(Buffer target, long uploadOffset, long targetOffset, long size) {}
}
