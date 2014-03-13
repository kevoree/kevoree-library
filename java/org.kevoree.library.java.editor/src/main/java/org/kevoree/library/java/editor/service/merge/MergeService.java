package org.kevoree.library.java.editor.service.merge;

import org.kevoree.ContainerRoot;
import org.kevoree.library.java.editor.model.Library;
import org.kevoree.library.java.editor.service.ServiceCallback;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 22/01/14
 * Time: 18:52
 */
public interface MergeService {
    
    ContainerRoot process(Collection<Library> libraries, Set<String> repos);
}
