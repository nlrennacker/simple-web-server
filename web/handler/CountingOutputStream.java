package web.handler;

import java.io.IOException;
import java.io.OutputStream;

public final class CountingOutputStream extends OutputStream {
    private OutputStream outputStream;
    protected int count;

    CountingOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void close() throws IOException {
        this.outputStream.close();
    }

    @Override
    public void flush() throws IOException {
        this.outputStream.flush();
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.outputStream.write(b);
        this.count += b.length;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.outputStream.write(b, off, len);
        this.count += len;
    }

    @Override
    public void write(int b) throws IOException {
        this.outputStream.write(b);
        this.count++;
    }

    public int getCount() {
        return this.count;
    }
}
