package org.kevoree.library;

import java.io.File;
import java.io.IOException;

/**
 * Created by mleduc on 22/12/15.
 */
public class GStreamerServiceTest {

    private final static GStreamerService gStreamerService = new GStreamerService();
    private final static B64Service b64Service = new B64Service();

    public static void main(String[] args) throws IOException {
        final String name = "/tmp/test.jpg";
        gStreamerService.picture(name);
        final String s = b64Service.fileToBase64(new File(name));
        System.out.println(s);
    }
}
