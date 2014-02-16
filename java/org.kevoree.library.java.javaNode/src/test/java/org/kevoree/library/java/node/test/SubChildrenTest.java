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
import org.kevoreeadaptation.AdaptationModel;


/**
 * Created by duke on 13/02/2014.
 */
public class SubChildrenTest {

    private KevScriptEngine engine = new KevScriptEngine();
    private DefaultKevoreeFactory factory = new DefaultKevoreeFactory();
    private KevoreeKompareBean kompare = new KevoreeKompareBean(new ModelRegistry());
    private DefaultModelCloner cloner = new DefaultModelCloner();

    private DefaultModelCompare modelCompare = new DefaultModelCompare();


    @Test
    public void childrenTest() throws Exception {

        ContainerRoot model = factory.createContainerRoot();
        TypeDefinition javaNode = factory.createNodeType();
        javaNode.setName("JavaNode");
        model.addTypeDefinitions(javaNode);
        engine.execute("add parentNode : JavaNode",model);

        ContainerRoot modelEmpty = (ContainerRoot) cloner.clone(model);

        engine.execute("add parentNode.child1 : JavaNode",model);
        engine.execute("add parentNode.child2 : JavaNode",model);

        TraceSequence sequence = modelCompare.diff(modelEmpty.findNodesByID("parentNode"), model.findNodesByID("parentNode"));
        AdaptationModel modelAdapt = kompare.compareModels(factory.createContainerRoot(), model, "parentNode");

        /*
        System.out.println(sequence);
        JSONModelSerializer saver = new JSONModelSerializer();
        saver.serializeToStream(modelAdapt, System.out);
        */
    }

}
