package org.kevoree.library.java.editor.cache;

import com.google.gson.JsonObject;
import org.kevoree.library.java.editor.cache.worker.AbstractWorker;
import org.kevoree.library.java.editor.cache.worker.JSWorker;
import org.kevoree.library.java.editor.cache.worker.MVNWorker;
import org.kevoree.log.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by leiko on 05/06/14.
 */
public class CacheHandler {

    private JsonObject jsLibs, javaLibs, cloudLibs;

    public CacheHandler(final int cacheDuration) {
        this.jsLibs = new JsonObject();
        this.javaLibs = new JsonObject();
        this.cloudLibs = new JsonObject();

        Thread reloadTask = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(60 * 1000 * cacheDuration);
                        loadAndWait();
                    }
                } catch (InterruptedException e) {
                    Log.error(e.getMessage());
                }
            }
        });
        reloadTask.start();
    }

    public void loadAndWait() {
        AbstractWorker jsWorker = new JSWorker();
        AbstractWorker javaWorker = new MVNWorker("java");
        AbstractWorker cloudWorker = new MVNWorker("cloud");

        ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.execute(jsWorker);
        executor.execute(javaWorker);
        executor.execute(cloudWorker);

        executor.shutdown();

        while (!executor.isTerminated()) {}

        this.jsLibs = jsWorker.getLibraries();
        this.javaLibs = javaWorker.getLibraries();
        this.cloudLibs = cloudWorker.getLibraries();
    }

    public JsonObject getJSLibs() {
        return this.jsLibs;
    }

    public JsonObject getJavaLibs() {
        return this.javaLibs;
    }

    public JsonObject getCloudLibs() {
        return this.cloudLibs;
    }
}