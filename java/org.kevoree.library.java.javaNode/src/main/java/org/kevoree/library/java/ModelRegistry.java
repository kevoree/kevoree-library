package org.kevoree.library.java;

import org.kevoree.pmodeling.api.KMFContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 29/11/2013
 * Time: 13:22
 */
public class ModelRegistry {

    protected Map<String, Object> registry = new HashMap<String, Object>();

    public Object lookup(KMFContainer elem) {
        return registry.get(elem.path());
    }

    public void register(KMFContainer elem, Object obj) {
        registry.put(elem.path(), obj);
    }

    public void registerFromPath(String path, Object obj) {
        registry.put(path, obj);
    }

    public void drop(KMFContainer elem) {
        registry.remove(elem.path());
    }

    public void clear() {
        registry.clear();
    }

    public static final ThreadLocal<ModelRegistry> current = new ThreadLocal<ModelRegistry>();

}
