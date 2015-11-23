package org.kevoree.library;

import org.junit.Assert;
import org.junit.Test;

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
        imaggaService.query(username, password, url);
    }

    //@Test(expected = ImaggaException.class)
    public void testUnsuportedImage() throws Exception {
        final String username = "";
        final String password = "";
        final String url = "http://weknowyourdreams.com/image.php?pic=/images/butterfly/butterfly-05.jpg";
        imaggaService.query(username, password, url);
    }

    //@Test
    public void test() throws Exception {
        final String username = "";
        final String password = "";
        final String url = "https://farm1.staticflickr.com/722/23104510232_d96706df46_m_d.jpg";
        imaggaService.query(username, password, url);
    }
}
