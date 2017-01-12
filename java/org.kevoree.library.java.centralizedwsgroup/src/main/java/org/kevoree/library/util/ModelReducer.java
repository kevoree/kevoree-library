package org.kevoree.library.util;

import org.kevoree.ContainerRoot;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.pmodeling.api.ModelCloner;

/**
 *
 * Created by leiko on 1/11/17.
 */
public class ModelReducer {

    public static ContainerRoot reduce(ContainerRoot model, String master, String client) {
        KevoreeFactory factory = new DefaultKevoreeFactory();
        ModelCloner cloner = factory.createModelCloner();
        ContainerRoot clonedModel = cloner.clone(model);
        // TODO
        return clonedModel;
    }
}
