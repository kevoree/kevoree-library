package org.kevoree.library.defaultNodeTypes.command;

import org.kevoree.ContainerNode;
import org.kevoree.DeployUnit;
import org.kevoree.Instance;
import org.kevoree.TypeDefinition;
import org.kevoree.api.BootstrapService;
import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.kcl.api.FlexyClassLoaderFactory;

/**
 * Created by duke on 5/23/14.
 */
public class ClassLoaderHelper {

    public static FlexyClassLoader createInstanceClassLoader(Instance c, String nodeName, BootstrapService bs) {

        if (c instanceof ContainerNode && ((ContainerNode) c).getHost() != null && ((ContainerNode) c).getHost().getName().equals(nodeName)) {
            return createTypeClassLoader(((ContainerNode) c).getHost().getTypeDefinition(), nodeName, bs);
        } else {
            return createTypeClassLoader(c.getTypeDefinition(), nodeName, bs);
        }
    }

    public static FlexyClassLoader createTypeClassLoader(TypeDefinition typeDefinition, String nodeName, BootstrapService bs) {
        FlexyClassLoader kcl = FlexyClassLoaderFactory.INSTANCE.create();
        for (DeployUnit du : typeDefinition.getDeployUnits()) {
            FlexyClassLoader resolved = bs.get(du);
            if (resolved != null) {
                kcl.attachChild(resolved);
            }
        }
        return kcl;
    }


}
