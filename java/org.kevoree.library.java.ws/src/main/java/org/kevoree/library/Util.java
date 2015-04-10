package org.kevoree.library;

import org.kevoree.Channel;
import org.kevoree.ContainerNode;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by leiko on 10/04/15.
 */
public class Util {

    public static Set<String> getProvidedPortsPath(Channel chan, String nodeName) {
        return getPortsPath(chan, nodeName, "provided");
    }

    public static Set<String> getRequiredPortsPath(Channel chan, String nodeName) {
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
}
