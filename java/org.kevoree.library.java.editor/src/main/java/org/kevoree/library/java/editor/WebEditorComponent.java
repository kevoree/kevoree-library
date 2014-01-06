package org.kevoree.library.java.editor;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.kevoree.annotation.*;
import org.kevoree.api.ModelService;
import org.kevoree.library.java.editor.handler.ClasspathResourceHandler;
import org.kevoree.library.java.editor.handler.IndexHandler;
import org.kevoree.library.java.editor.handler.InitHandler;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 25/11/13
 * Time: 12:37
 */
@ComponentType
@Library(name = "Java :: Components")
public class WebEditorComponent {

    @Param(optional = false, defaultValue = "3042")
    private Integer port = 3042;

    @KevoreeInject
    public ModelService modelService;
    
    private Server server;

    @Start
    public void start() throws Exception {
        server = new Server(port);

        ResourceHandler resourceHandler = new ClasspathResourceHandler();
        resourceHandler.setResourceBase("webapp/public");

        ContextHandler initContext = new ContextHandler();
        initContext.setContextPath("/init");
        initContext.setHandler(new InitHandler(modelService));

        ContextHandler indexContext = new ContextHandler();
        indexContext.setContextPath("/");
        indexContext.setHandler(new IndexHandler());

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resourceHandler, initContext, indexContext });
        server.setHandler(handlers);

        server.start();
    }

    @Stop
    public void stop() throws Exception {
        if (server != null) server.stop();
    }
}
