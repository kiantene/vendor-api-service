package com.nextgen.gameaggregator.core.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A request wrapper that caches the request body and allows multiple reads.
 * This enables signature validation in filters while preserving @RequestBody functionality.
 */
public class ResettableRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;
    private final String encoding;

    /**
     * Creates a wrapper that immediately reads and caches the request body.
     *
     * @param request the original HTTP request
     * @throws IOException if reading the request body fails
     */
    public ResettableRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);

        // Determine character encoding
        this.encoding = request.getCharacterEncoding() != null ?
                request.getCharacterEncoding() : StandardCharsets.UTF_8.name();

        // Read and cache the entire request body immediately
        try (InputStream inputStream = request.getInputStream()) {
            this.cachedBody = inputStream.readAllBytes();
        }
    }

    /**
     * Returns a fresh ServletInputStream that reads from the cached body.
     * This allows multiple reads of the same body content.
     */
    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    /**
     * Returns a fresh BufferedReader that reads from the cached body.
     * This allows multiple reads of the same body content.
     */
    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(this.cachedBody), encoding));
    }

    /**
     * Gets the cached request body as a string using the request's character encoding.
     * Useful for signature validation and logging.
     *
     * @return the request body as a string
     */
    public String getCachedBody() {
        return new String(this.cachedBody, Charset.forName(encoding));
    }

    /**
     * Gets the cached request body as a byte array.
     * Useful for binary content or cryptographic operations.
     *
     * @return a copy of the cached body bytes
     */
    public byte[] getCachedBodyBytes() {
        return Arrays.copyOf(this.cachedBody, this.cachedBody.length);
    }

    /**
     * Returns the size of the cached body in bytes.
     *
     * @return the body size
     */
    public int getBodySize() {
        return this.cachedBody.length;
    }

    /**
     * Checks if the request has a body (non-empty).
     *
     * @return true if body exists and is not empty
     */
    public boolean hasBody() {
        return this.cachedBody.length > 0;
    }
}

class CachedBodyServletInputStream extends ServletInputStream {

    private final ByteArrayInputStream inputStream;
    private boolean finished = false;

    public CachedBodyServletInputStream(byte[] cachedBody) {
        this.inputStream = new ByteArrayInputStream(cachedBody);
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public boolean isReady() {
        return true; // Always ready since data is in memory
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException("Async reading not supported");
    }

    @Override
    public int read() {
        int data = inputStream.read();
        if (data == -1) {
            finished = true;
        }
        return data;
    }

    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        int readBytes = inputStream.read(b, off, len);
        if (readBytes == -1) {
            finished = true;
        }
        return readBytes;
    }

    @Override
    public int available() {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        super.close();
        inputStream.close();
        finished = true;
    }

    @Override
    public void mark(int readLimit) {
        inputStream.mark(readLimit);
    }

    @Override
    public void reset() {
        inputStream.reset();
        finished = false;
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }
}
