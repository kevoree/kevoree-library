package org.kevoree.library.ws;

import org.junit.Test;
import org.kevoree.tools.test.KevoreeTestCase;

/**
 * Created by duke on 3/16/14.
 */
public class WSGroupTest extends KevoreeTestCase {

    @Test
    public void test() throws Exception {
        bootstrap("node0", "dicfrag.kevs");
        waitLog("node0", ".*WSGroup listen on 9000", 10000);
        waitLog("node0", ".*WSGroup listen on 9001", 10000);

        //waitLog("node1", ".*WSGroup listen on 9001", 10000);

        Thread.sleep(5000);

    }

}
