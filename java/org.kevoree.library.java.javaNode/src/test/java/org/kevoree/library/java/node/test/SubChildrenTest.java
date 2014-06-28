package org.kevoree.library.java.node.test;

import org.junit.Test;
import org.kevoree.tools.test.KevoreeTestCase;

/**
 * Created by duke on 13/02/2014.
 */
public class SubChildrenTest extends KevoreeTestCase {
    @Test
    public void startupChildTest() throws Exception {
        bootstrap("node0", "oneChild.kevs");
        waitLog("node0", "node0/child1/* INFO: Bootstrap completed", 10000);

        exec("node0", "stop child1");
        assert (getCurrentModel("node0").findNodesByID("child1").getStarted() == false);
        waitLog("node0", "node0/* INFO: Stopping nodes[child1]", 5000);
        exec("node0", "start child1");
        assert (getCurrentModel("node0").findNodesByID("child1").getStarted() == true);
        waitLog("node0", "node0/child1/* INFO: Bootstrap completed", 10000);
    }
}
