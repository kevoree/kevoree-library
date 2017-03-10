package org.kevoree.library;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kevoree.ContainerRoot;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.api.ModelService;
import org.kevoree.api.RuntimeService;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.compare.AdaptationEngine;
import org.kevoree.library.wrapper.WrapperFactory;
import org.kevoree.log.Log;
import org.mockito.Mockito;

import java.util.List;

/**
 *
 * Created by leiko on 3/2/17.
 */
public class TestAdaptationEngine {

    private KevoreeFactory factory = new DefaultKevoreeFactory();
    private WrapperFactory wrapperFactory;
    private InstanceRegistry registry;
    private ModelService model;
    private RuntimeService runtime;

    @Before
    public void setUp() {
        this.wrapperFactory = Mockito.mock(WrapperFactory.class);
        this.registry = Mockito.mock(InstanceRegistry.class);

        this.model = Mockito.mock(ModelService.class);

        this.runtime = Mockito.mock(RuntimeService.class);
    }

    @Test
    public void testPlan() throws KevoreeAdaptationException {
        Log.set(Log.LEVEL_TRACE);
        AdaptationEngine engine = new AdaptationEngine("node0", model, runtime, registry, wrapperFactory);

        ContainerRoot srcModel = factory.createContainerRoot();
        ContainerRoot targetModel = (ContainerRoot) factory.createJSONLoader()
                .loadModelFromStream(getClass().getResourceAsStream("/model.json")).get(0);
        List<AdaptationCommand> cmds = engine.plan(srcModel, targetModel);

        // TODO improve assertion because only testing size sucks
        Assert.assertEquals(16, cmds.size());
    }

    @Test
    public void testUpdateInstanceRemovedWhenStopInstance() throws KevoreeAdaptationException {
        Log.set(Log.LEVEL_TRACE);
        AdaptationEngine engine = new AdaptationEngine("node0", model, runtime, registry, wrapperFactory);

        ContainerRoot srcModel = (ContainerRoot) factory.createJSONLoader()
                .loadModelFromStream(getClass().getResourceAsStream("/first.json")).get(0);
        ContainerRoot targetModel = (ContainerRoot) factory.createJSONLoader()
                .loadModelFromStream(getClass().getResourceAsStream("/second.json")).get(0);

        List<AdaptationCommand> cmds = engine.plan(srcModel, targetModel);

        // TODO improve assertion because only testing size sucks
        Assert.assertEquals(10, cmds.size());
    }
}
