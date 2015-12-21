package org.kevoree.library;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import sun.nio.ch.IOUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;

/**
 * Created by mleduc on 18/11/15.
 */
public class ImaggaServiceTest {

    private final ImaggaService imaggaService = new ImaggaService();

    @Test(expected = ImaggaException.class)
    public void testWrongCreds() throws Exception {
        final String username = "a";
        final String password = "b";
        final String url = "http://weknowyourdreams.com/image.php?pic=/images/butterfly/butterfly-05.jpg";
        imaggaService.query(username, password, url, false);
    }

    //@Test(expected = ImaggaException.class)
    public void testUnsuportedImage() throws Exception {
        final String username = "";
        final String password = "";
        final String url = "http://weknowyourdreams.com/image.php?pic=/images/butterfly/butterfly-05.jpg";
        imaggaService.query(username, password, url, false);
    }

    //@Test
    public void test() throws Exception {
        final String username = "";
        final String password = "";
        final String url = "https://farm1.staticflickr.com/722/23104510232_d96706df46_m_d.jpg";
        imaggaService.query(username, password, url, false);
    }

    @Test
    public void testContent() throws Exception {
        final String username = "acc_c4e0e651f7229ff";
        final String password = "493f8f3d4309468ded707e04652cab6d";
        final File file = new File("/tmp/test2.jpg");
        final FileInputStream fileInputStream = new FileInputStream(file);
        final byte[] bytes = IOUtils.toByteArray(fileInputStream);
        final String s = Base64.getEncoder().encodeToString(bytes);
        imaggaService.query(username, password, s, true);
    }
}
