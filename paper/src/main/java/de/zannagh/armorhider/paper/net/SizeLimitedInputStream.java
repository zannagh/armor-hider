package de.zannagh.armorhider.paper.net;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Fails fast once more than {@code limit} bytes have been read, so an over-large stream is rejected
 * during inflation rather than after it has already been materialised in memory.
 *
 * <p>Mirrors the guard in the mod's {@code CompressedJsonCodec}: bounding the <em>compressed</em>
 * length is not enough on its own, because gzip routinely hits ~38x on this data and a crafted
 * stream reaches ~1000x.</p>
 */
final class SizeLimitedInputStream extends FilterInputStream {

    private final long limit;
    private long consumed;

    SizeLimitedInputStream(InputStream delegate, long limit) {
        super(delegate);
        this.limit = limit;
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value != -1) {
            recordRead(1);
        }
        return value;
    }

    @Override
    public int read(byte[] buffer, int off, int len) throws IOException {
        int readCount = super.read(buffer, off, len);
        recordRead(readCount);
        return readCount;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = super.skip(n);
        recordRead(skipped);
        return skipped;
    }

    private void recordRead(long readCount) throws IOException {
        if (readCount <= 0) {
            return;
        }
        consumed += readCount;
        if (consumed > limit) {
            throw new IOException("Rejecting an armor-hider payload that inflates beyond " + limit
                    + " bytes - refusing to decompress further");
        }
    }
}
