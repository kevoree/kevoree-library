package org.kevoree.library.util;

import org.kevoree.api.Port;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * Created by leiko on 10/11/15.
 */
public class PortStreamer extends OutputStream {

    private Port port;
    private ByteArrayOutputStream out;

    public PortStreamer(Port port) {
        this.port = port;
        this.out = new ByteArrayOutputStream();
    }

    @Override
    public void write(int b) throws IOException {
        if (b == '\n') {
            port.send(out.toString(), null);
            out.reset();
        } else {
            out.write(b);
        }
    }
}
