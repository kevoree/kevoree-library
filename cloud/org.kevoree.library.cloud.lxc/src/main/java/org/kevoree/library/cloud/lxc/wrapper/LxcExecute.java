package org.kevoree.library.cloud.lxc.wrapper;

import org.kevoree.ContainerNode;
import org.kevoree.library.cloud.lxc.wrapper.utils.FileManager;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 06/12/13
 * Time: 17:59
 * To change this template use File | Settings | File Templates.
 */
public class LxcExecute 
{

    void start(ContainerNode node) throws IOException, InterruptedException {

        Process processcreate = new ProcessBuilder(LxcContants.lxcexecute, "-n", node.getName(), "").redirectErrorStream(true).start();
        FileManager.display_message_process(processcreate.getInputStream());
        processcreate.waitFor();
    }

    void stop(ContainerNode node){


    }
    
}
