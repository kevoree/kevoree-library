package org.kevoree.library.java.editor.cache.worker;

import com.google.gson.JsonObject;

/**
 * Created by leiko on 05/06/14.
 */
public abstract class AbstractWorker implements Runnable {

    protected JsonObject libraries = new JsonObject();

    public JsonObject getLibraries() {
        return this.libraries;
    }
}
