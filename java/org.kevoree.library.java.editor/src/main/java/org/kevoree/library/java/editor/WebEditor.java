package org.kevoree.library.java.editor;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.kevoree.annotation.*;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.ModelService;
import org.kevoree.library.java.editor.cache.CacheHandler;
import org.kevoree.library.java.editor.handler.ClasspathResourceHandler;
import org.kevoree.library.java.editor.handler.LoadHandler;
import org.kevoree.library.java.editor.handler.MergeHandler;
import org.kevoree.log.Log;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 25/11/13
 * Time: 12:37
 */
@ComponentType
@Library(name = "Java")
public class WebEditor {

    @Param(optional = false, defaultValue = "3042")
    private Integer port;

    @Param(optional = false, defaultValue = "30")
    private int cacheDuration;

    @KevoreeInject
    public ModelService modelService;
    
    private Server server;

    @KevoreeInject
    BootstrapService bootstrapService;

    @Start
    public void start() throws Exception {
        Log.debug("WebEditor START");
        server = new Server(port);

        // caching java, cloud & javascript libraries
        CacheHandler cacheHandler = new CacheHandler(cacheDuration);

        Handler resourceHandler = new ClasspathResourceHandler();
        Handler loadHandler = new LoadHandler(cacheHandler);
        Handler mergeHandler = new MergeHandler(bootstrapService);

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resourceHandler, loadHandler, mergeHandler });

        server.setHandler(handlers);

        Log.info("Kevoree standard libraries: preparing caches...(this may take a while)");
        cacheHandler.load();
        server.start();
        Log.info("Kevoree Web Editor Service: started on port {}", port);
    }

    @Stop
    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }
}
