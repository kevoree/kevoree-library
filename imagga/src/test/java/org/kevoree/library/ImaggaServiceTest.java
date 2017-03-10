package org.kevoree.library;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;

/**
 * Created by mleduc on 18/11/15.
 */
public class ImaggaServiceTest {

    private final ImaggaService imaggaService = new ImaggaService();
    private final SerializerService serializerService = new SerializerService();

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
        final ImaggaTagSet query = imaggaService.query(username, password, url, false);
        final String s1 = serializerService.toJson(query);
        System.out.println(s1);
    }

    //@Test
    public void testContent() throws Exception {
        final String username = "";
        final String password = "";
        final File file = new File("/tmp/test.jpg");
        final FileInputStream fileInputStream = new FileInputStream(file);
        final byte[] bytes = IOUtils.toByteArray(fileInputStream);
        final String s = Base64.getEncoder().encodeToString(bytes);
        final ImaggaTagSet query = imaggaService.query(username, password, s, true);
        final String s1 = serializerService.toJson(query);
        System.out.println(s1);
    }
}
