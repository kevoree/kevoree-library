package org.kevoree.library.java.editor;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.kevoree.annotation.*;
import org.kevoree.library.java.editor.handler.ClasspathResourceHandler;
import org.kevoree.log.Log;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 25/11/13
 * Time: 12:37
 */
@ComponentType
public class WebEditor {

    @Param(optional = false, defaultValue = "3042")
    private Integer port;

    private Server server;

    @Start
    public void start() throws Exception {
        Log.debug("WebEditor START");

        server = new Server(port);
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { new ClasspathResourceHandler() });
        server.setHandler(handlers);
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
