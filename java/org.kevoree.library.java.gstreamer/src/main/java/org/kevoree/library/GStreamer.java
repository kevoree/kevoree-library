package org.kevoree.library;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.Output;
import org.kevoree.annotation.Start;
import org.kevoree.api.Port;
import org.kevoree.log.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by mleduc on 22/12/15.
 */
@ComponentType(version = 1)
public class GStreamer {
    @Output
    private Port out;

    private final GStreamerService gStreamerService = new GStreamerService();
    private final B64Service b64Service = new B64Service();
    private File file;

    @Start
    public void start() {
        try {
            this.file = File.createTempFile("gstreamer", ".jpg");
        } catch (IOException e) {
            Log.error(e.getMessage());
        }

    }

    @Input
    public void in(final String ticker) {
        try {
            this.gStreamerService.picture(file);
            this.out.send(b64Service.fileToBase64(file));
        } catch (IOException e) {
            Log.error(e.getMessage());
        }
    }
}
