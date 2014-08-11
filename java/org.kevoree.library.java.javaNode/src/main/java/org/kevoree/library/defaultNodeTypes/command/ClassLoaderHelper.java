package org.kevoree.library.defaultNodeTypes.command;

import org.kevoree.ContainerNode;
import org.kevoree.Instance;
import org.kevoree.library.defaultNodeTypes.ModelRegistry;

/**
 * Created by duke on 5/23/14.
 */
public class ClassLoaderHelper {

    public static ClassLoader getClassLoader(ModelRegistry registry, Instance c, String nodeName) {

        if (c instanceof ContainerNode && ((ContainerNode) c).getHost() != null && ((ContainerNode) c).getHost().getName().equals(nodeName)) {
            return (ClassLoader) registry.lookup(((ContainerNode) c).getHost().getTypeDefinition());
        } else {
            return (ClassLoader) registry.lookup(c.getTypeDefinition());
        }
    }


}
