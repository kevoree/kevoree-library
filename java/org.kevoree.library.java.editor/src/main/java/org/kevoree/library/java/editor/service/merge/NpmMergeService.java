package org.kevoree.library.java.editor.service.merge;

import org.kevoree.ContainerRoot;
import org.kevoree.impl.DefaultKevoreeFactory;
import org.kevoree.library.java.editor.model.Library;

import java.util.Collection;

/**
 * Created by leiko on 23/01/14.
 */
public class NpmMergeService implements MergeService {

    @Override
    public ContainerRoot process(Collection<Library> libraries) {
        DefaultKevoreeFactory factory = new DefaultKevoreeFactory();
        ContainerRoot model = factory.createContainerRoot();
        return model;
    }
}
