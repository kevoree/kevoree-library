package org.kevoree.library.cloud.docker;

import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.Info;
import org.apache.commons.lang.UnhandledException;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.NodeType;
import org.kevoree.annotation.Update;
import org.kevoree.api.ModelService;
import org.kevoree.library.cloud.docker.wrapper.DockerWrapperFactory;
import org.kevoree.library.defaultNodeTypes.JavaNode;
import org.kevoree.library.defaultNodeTypes.wrapper.WrapperFactory;
import org.kevoree.log.Log;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 10:17
 */
@NodeType
public class DockerNode extends JavaNode {

    @KevoreeInject
    ModelService modelsService;

    @Override
    protected WrapperFactory createWrapperFactory(String nodeName) {
        System.out.println("CREATE WRAPPER FACTORY");
        return new DockerWrapperFactory(nodeName);
    }

    @Override
    public void startNode() {
        super.startNode();
        System.out.println("START NODE");
        DockerClient docker = new DockerClient("http://localhost:4243");
        try {
            Info info = docker.info();
            Log.debug(info.toString());
        } catch (DockerException e) {
            e.printStackTrace();
        }
    }

    @Update
    public void updateNode() {
        System.out.println("UPDATE");
    }

    @Override
    public void stopNode() {
        super.stopNode();
        System.out.println("STOP NODE");
    }
}