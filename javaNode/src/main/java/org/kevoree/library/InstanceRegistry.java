package org.kevoree.library;

import org.kevoree.modeling.api.KMFContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 29/11/2013
 * Time: 13:22
 */
public class InstanceRegistry {

    protected Map<String, Object> registry = new HashMap<>();

    public Object get(KMFContainer elem) {
        return registry.get(elem.path());
    }

    public <T> T get(KMFContainer elem, Class<T> clazz) {
        return clazz.cast(get(elem));
    }

    public void put(KMFContainer elem, Object obj) {
        registry.put(elem.path(), obj);
    }

    public void remove(KMFContainer elem) {
        registry.remove(elem.path());
    }

    public void clear() {
        registry.clear();
    }
}
