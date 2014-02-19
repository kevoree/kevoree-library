package org.kevoree.library.java.node.test;

import org.junit.Test;
import org.kevoree.*;
import org.kevoree.cloner.DefaultModelCloner;
import org.kevoree.compare.DefaultModelCompare;
import org.kevoree.impl.DefaultKevoreeFactory;
import org.kevoree.kevscript.KevScriptEngine;
import org.kevoree.library.defaultNodeTypes.ModelRegistry;
import org.kevoree.library.defaultNodeTypes.planning.KevoreeKompareBean;
import org.kevoree.modeling.api.trace.TraceSequence;
import org.kevoree.serializer.JSONModelSerializer;
import org.kevoree.tools.test.KevoreeTestCase;
import org.kevoreeadaptation.AdaptationModel;


/**
 * Created by duke on 13/02/2014.
 */
public class SubChildrenTest extends KevoreeTestCase {

    @Test
    public void startupChildTest() throws Exception {
        bootstrap("node0", "oneChild.kevs");
        exec("node0", "set child1.started = \"false\"");
        assert(getCurrentModel("node0").findNodesByID("child1").getStarted() == false);
        exec("node0", "set child1.started = \"true\"");
        assert(getCurrentModel("node0").findNodesByID("child1").getStarted() == true);

    }

}
