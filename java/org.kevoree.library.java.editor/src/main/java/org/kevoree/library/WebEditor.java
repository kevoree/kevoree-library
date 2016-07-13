package org.kevoree.library;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Param;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.kcl.impl.FlexyClassLoaderImpl;
import org.kevoree.log.Log;
import org.xnio.Xnio;
import org.xnio.XnioProvider;

import java.util.ServiceLoader;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 25/11/13
 * Time: 12:37
 */
@ComponentType(version = 1)
public class WebEditor {

    @Param(optional = false, defaultValue = "3042")
    private Integer port;

    private Undertow server;

    @Start
    public void start() throws Exception {
        server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setHandler(new ResourceHandler(new ClassPathResourceManager(this.getClass().getClassLoader(), "static/")) {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        super.handleRequest(exchange);
                    }
                }).build();
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
