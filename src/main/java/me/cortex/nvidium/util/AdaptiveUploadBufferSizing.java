package me.cortex.nvidium.util;

/**
 * Chooses the size of Nvidium's persistent CPU-to-GPU geometry upload buffer.
 *
 * <p>The buffer is native/client storage rather than Java heap memory. The configured
 * maximum heap is used only as a conservative signal for how much system memory the
 * user intended to make available to Minecraft.</p>
 */
public final class AdaptiveUploadBufferSizing {
    private static final long MEBIBYTE = 1024L * 1024L;

    public static final int MIN_UPLOAD_BUFFER_MB = 32;
    public static final int MAX_UPLOAD_BUFFER_MB = 512;

    private AdaptiveUploadBufferSizing() {
    }

    public static long resolveBytes(boolean automatic, int configuredMegabytes) {
        int megabytes = automatic
                ? automaticMegabytes(Runtime.getRuntime().maxMemory())
                : clampMegabytes(configuredMegabytes);

        return megabytes * MEBIBYTE;
    }

    static int automaticMegabytes(long maximumHeapBytes) {
        long maximumHeapMegabytes = Math.max(1L, maximumHeapBytes / MEBIBYTE);

        // Reserve up to 6.25% of the configured heap limit as native upload staging.
        // 4 GiB heap -> 256 MiB, 6 GiB -> 384 MiB, 8 GiB+ -> 512 MiB.
        long targetMegabytes = maximumHeapMegabytes / 16L;
        int clamped = clampMegabytes((int) Math.min(Integer.MAX_VALUE, targetMegabytes));

        // Keep the allocation aligned to 16 MiB for predictable driver allocations.
        return Math.max(MIN_UPLOAD_BUFFER_MB, (clamped / 16) * 16);
    }

    private static int clampMegabytes(int megabytes) {
        return Math.max(MIN_UPLOAD_BUFFER_MB, Math.min(MAX_UPLOAD_BUFFER_MB, megabytes));
    }
}
