package org.kevoree.library;

import org.kevoree.Channel;
import org.kevoree.MBinding;
import org.kevoree.ContainerNode;
import org.kevoree.api.Port;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by leiko on 10/04/15.
 */
public class Util {

    public static Set<String> getInputPath(Channel chan, String nodeName) {
        return getPortsPath(chan, nodeName, "provided");
    }

    public static Set<String> getOutputPath(Channel chan, String nodeName) {
        return getPortsPath(chan, nodeName, "required");
    }

    private static Set<String> getPortsPath(Channel chan, String nodeName, String type) {
        Set<String> paths = new HashSet<String>();
        if (chan != null) {
            chan.getBindings().stream().filter(binding -> binding.getPort() != null
                    && binding.getPort().getRefInParent() != null
                    && binding.getPort().getRefInParent().equals(type)).forEach(binding -> {
                ContainerNode node = (ContainerNode) binding.getPort().eContainer().eContainer();
                if (node.getName().equals(nodeName)) {
                    paths.add(binding.getPort().path());
                }
            });
        }
        return paths;
    }

   /* private static MBinding findPortByName(Channel chan, String nodeName, String path, String type) {
            return chan.getBindings().stream().filter(binding -> binding.getPort() != null
                    && binding.getPort().getRefInParent() != null
                    && binding.getPort().getRefInParent().equals(type)).filter(binding -> {
                        ContainerNode node = (ContainerNode) binding.getPort().eContainer().eContainer();
                        return node.getName().equals(nodeName) && binding.getPort().path().equals(path);
                    }).findFirst().orElseGet(null);
    }*/
}
