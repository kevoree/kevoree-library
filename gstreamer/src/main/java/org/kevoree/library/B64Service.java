package org.kevoree.library;

import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by mleduc on 22/12/15.
 */
public class B64Service {
    public String fileToBase64(final File file) throws IOException {
        final byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(new FileInputStream(file));
        final byte[] encoded = Base64.encodeBase64(bytes);
        return new String(encoded);
    }
}
