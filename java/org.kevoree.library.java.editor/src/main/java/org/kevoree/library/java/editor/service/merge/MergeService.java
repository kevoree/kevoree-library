package org.kevoree.library.java.editor.service.merge;

import org.kevoree.ContainerRoot;
import org.kevoree.library.java.editor.model.Library;
import org.kevoree.library.java.editor.service.ServiceCallback;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 22/01/14
 * Time: 18:52
 * To change this template use File | Settings | File Templates.
 */
public interface MergeService {
    
    ContainerRoot process(Collection<Library> libraries);
}
