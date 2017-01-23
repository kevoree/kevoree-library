package org.kevoree.library.util;

import org.kevoree.ContainerRoot;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.pmodeling.api.json.JSONModelLoader;

/**
 *
 * Created by leiko on 1/12/17.
 */
public class ModelHelper {

    public static ContainerRoot readModel(String modelFile) {
        KevoreeFactory factory = new DefaultKevoreeFactory();
        JSONModelLoader loader = factory.createJSONLoader();
        return (ContainerRoot) loader.loadModelFromStream(ModelHelper.class.getResourceAsStream(modelFile)).get(0);
    }
}
