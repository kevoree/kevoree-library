package org.kevoree.library;

import org.junit.Test;
import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.util.ModelHelper;
import org.kevoree.library.util.ModelReducer;
import org.kevoree.pmodeling.api.json.JSONModelSerializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 * Created by leiko on 1/12/17.
 */
public class TestReducer {

    private final KevoreeFactory factory = new DefaultKevoreeFactory();
    private final JSONModelSerializer saver = factory.createJSONSerializer();

    @Test
    public void testEmptyModel() {
        ContainerRoot model = factory.createContainerRoot();
        ContainerRoot reducedModel = ModelReducer.reduce(model, "master", "client");

        assertEquals(saver.serialize(model), saver.serialize(reducedModel));
    }

    @Test
    public void testDoNotRemoveUnwantedProps() {
        ContainerRoot model = ModelHelper.readModel("/simple-client.json");
        ContainerRoot reducedModel = ModelReducer.reduce(model, "node0", "node1");

        ContainerNode node0 = reducedModel.findNodesByID("node0");
        ContainerNode node1 = reducedModel.findNodesByID("node1");

        assertNotNull(node0);
        assertNotNull(node0.getTypeDefinition());
        assertEquals(node0.getTypeDefinition().getDeployUnits().size(), 1);

        assertNotNull(node1);
        assertNotNull(node1.getTypeDefinition());
        assertEquals(node1.getTypeDefinition().getDeployUnits().size(), 1);
    }
}
