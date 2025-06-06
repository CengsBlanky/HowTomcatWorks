package org.apache.catalina.connector.http;

import org.apache.catalina.connector.RequestStream;

import java.io.IOException;

/**
 *
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @deprecated
 */
public class HttpRequestStream extends RequestStream {
    // ----------------------------------------------------------- Constructors

    /**
     * Construct a servlet input stream associated with the specified Request.
     *
     * @param request The associated request
     * @param response The associated response
     */
    public HttpRequestStream(HttpRequestImpl request, HttpResponseImpl response) {
        super(request);
        String transferEncoding = request.getHeader("Transfer-Encoding");

        http11 = request.getProtocol().equals("HTTP/1.1");
        chunk = ((transferEncoding != null) && (transferEncoding.indexOf("chunked") != -1));

        if ((!chunk) && (length == -1)) {
            // Ask for connection close
            response.addHeader("Connection", "close");
        }
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * Use chunking ?
     */
    protected boolean chunk = false;

    /**
     * True if the final chunk was found.
     */
    protected boolean endChunk = false;

    /**
     * Chunk buffer.
     */
    protected byte[] chunkBuffer = null;

    /**
     * Chunk length.
     */
    protected int chunkLength = 0;

    /**
     * Chunk buffer position.
     */
    protected int chunkPos = 0;

    /**
     * HTTP/1.1 flag.
     */
    protected boolean http11 = false;

    // --------------------------------------------------------- Public Methods

    /**
     * Close this input stream.  No physical level I-O is performed, but
     * any further attempt to read from this stream will throw an IOException.
     * If a content length has been set but not all of the bytes have yet been
     * consumed, the remaining bytes will be swallowed.
     */
    public void close() throws IOException {
        if (closed)
            throw new IOException(sm.getString("requestStream.close.closed"));

        if (chunk) {
            while (!endChunk) {
                int b = read();
                if (b < 0)
                    break;
            }

        } else {
            if (http11 && (length > 0)) {
                while (count < length) {
                    int b = read();
                    if (b < 0)
                        break;
                }
            }
        }

        closed = true;
    }

    /**
     * Read and return a single byte from this input stream, or -1 if end of
     * file has been encountered.
     *
     * @exception IOException if an input/output error occurs
     */
    public int read() throws IOException {
        // Has this stream been closed?
        if (closed)
            throw new IOException(sm.getString("requestStream.read.closed"));

        if (chunk) {
            if (endChunk)
                return (-1);

            if ((chunkBuffer == null) || (chunkPos >= chunkLength)) {
                if (!fillChunkBuffer())
                    return (-1);
            }

            return (chunkBuffer[chunkPos++] & 0xff);

        } else {
            return (super.read());
        }
    }

    /**
     * Read up to <code>len</code> bytes of data from the input stream
     * into an array of bytes.  An attempt is made to read as many as
     * <code>len</code> bytes, but a smaller number may be read,
     * possibly zero.  The number of bytes actually read is returned as
     * an integer.  This method blocks until input data is available,
     * end of file is detected, or an exception is thrown.
     *
     * @param b The buffer into which the data is read
     * @param off The start offset into array <code>b</code> at which
     *  the data is written
     * @param len The maximum number of bytes to read
     *
     * @exception IOException if an input/output error occurs
     */
    public int read(byte b[], int off, int len) throws IOException {
        if (chunk) {
            int avail = chunkLength - chunkPos;
            if (avail == 0)
                fillChunkBuffer();
            avail = chunkLength - chunkPos;
            if (avail == 0)
                return (-1);

            int toCopy = avail;
            if (avail > len)
                toCopy = len;
            System.arraycopy(chunkBuffer, chunkPos, b, off, toCopy);
            chunkPos += toCopy;
            return toCopy;

        } else {
            return super.read(b, off, len);
        }
    }

    // -------------------------------------------------------- Private Methods

    /**
     * Fill the chunk buffer.
     */
    private synchronized boolean fillChunkBuffer() throws IOException {
        chunkPos = 0;

        try {
            String numberValue = readLineFromStream();
            if (numberValue != null)
                numberValue = numberValue.trim();
            chunkLength = Integer.parseInt(numberValue, 16);
        } catch (NumberFormatException e) {
            // Critical error, unable to parse the chunk length
            chunkLength = 0;
            chunk = false;
            close();
            return false;
        }

        if (chunkLength == 0) {
            // Skipping trailing headers, if any
            String trailingLine = readLineFromStream();
            while (!trailingLine.equals("")) trailingLine = readLineFromStream();
            endChunk = true;
            return false;
            // TODO : Should the stream be automatically closed ?

        } else {
            if ((chunkBuffer == null) || (chunkLength > chunkBuffer.length))
                chunkBuffer = new byte[chunkLength];

            // Now read the whole chunk into the buffer

            int nbRead = 0;
            int currentRead = 0;

            while (nbRead < chunkLength) {
                try {
                    currentRead = stream.read(chunkBuffer, nbRead, chunkLength - nbRead);
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw new IOException();
                }
                if (currentRead < 0) {
                    throw new IOException(sm.getString("requestStream.read.error"));
                }
                nbRead += currentRead;
            }

            // Skipping the CRLF
            String blank = readLineFromStream();
        }

        return true;
    }

    /**
     * Reads the input stream, one line at a time. Reads bytes into an array,
     * until it reads a certain number of bytes or reaches a newline character,
     * which it reads into the array as well.
     *
     * @param input Input stream on which the bytes are read
     * @return The line that was read, or <code>null</code> if end-of-file
     *  was encountered
     * @exception IOException   if an input or output exception has occurred
     */
    private String readLineFromStream() throws IOException {
        StringBuffer sb = new StringBuffer();
        while (true) {
            int ch = super.read();
            if (ch < 0) {
                if (sb.length() == 0) {
                    return (null);
                } else {
                    break;
                }
            } else if (ch == '\r') {
                continue;
            } else if (ch == '\n') {
                break;
            }
            sb.append((char) ch);
        }
        return (sb.toString());
    }
}
