package hugo.weaving.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 同时输出到文件和终端
 */
public class MultiOutputStream extends OutputStream {
    OutputStream[] outputStreams;

    public MultiOutputStream(OutputStream outputStream, String logPath) {
        try {
            FileOutputStream out = new FileOutputStream(logPath);
            this.outputStreams = new OutputStream[] { outputStream, out };
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void write(int b) throws IOException {
        for (OutputStream out : outputStreams)
            out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        for (OutputStream out : outputStreams)
            out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (OutputStream out : outputStreams)
            out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        for (OutputStream out : outputStreams)
            out.flush();
    }

    @Override
    public void close() throws IOException {
        for (OutputStream out : outputStreams)
            out.close();
    }
}