package org.kevoree.library.defaultNodeTypes;

import org.kevoree.ContainerRoot;
import org.kevoree.library.defaultNodeTypes.planning.KevoreeKompareBean;
import org.kevoree.loader.JSONModelLoader;
import org.kevoreeadaptation.AdaptationModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 14:29
 */

public class Tester {

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("Top");
        JSONModelLoader loader = new JSONModelLoader();

        ContainerRoot modelBefore = (ContainerRoot) loader.loadModelFromStream(new FileInputStream(new File("/Users/duke/Downloads/ModelAvant.json"))).get(0);
        ContainerRoot modelAfter = (ContainerRoot) loader.loadModelFromStream(new FileInputStream(new File("/Users/duke/Downloads/ModelApres.json"))).get(0);

        KevoreeKompareBean bean = new KevoreeKompareBean(new ModelRegistry());
        AdaptationModel model = bean.compareModels(modelBefore, modelAfter, "node0");

    }

}
