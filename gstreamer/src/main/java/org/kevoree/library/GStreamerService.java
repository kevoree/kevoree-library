package org.kevoree.library;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.ds.gstreamer.GStreamerDriver;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Created by mleduc on 22/12/15.
 */
public class GStreamerService {

    static {
        Webcam.setDriver(new GStreamerDriver());
    }

    public void picture(final String name) throws IOException {
        picture(new File(name));
    }

    public void picture(final File output) throws IOException {
        final Webcam webcam = Webcam.getDefault();
        webcam.open();
        ImageIO.write(webcam.getImage(), "JPG", output);
    }
}
