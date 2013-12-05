package org.kevoree.library.web.jetty;

import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.webbitserver.*;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 05/12/2013
 * Time: 10:35
 */

@ComponentType
public class BlogServer2 implements HttpHandler {

    WebServer webServer;

    @Param(defaultValue = "8080")
    Integer http_port;

    @KevoreeInject
    Context context;

    @Start
    public void start() {
        webServer = WebServers.createWebServer(http_port);
        webServer.add(this);
        webServer.start();
    }

    @Stop
    public void stop() {
        webServer.stop();
    }

    @Override
    public void handleHttpRequest(HttpRequest httpRequest, HttpResponse httpResponse, HttpControl httpControl) throws Exception {

        httpResponse.content("hello from " + context.getInstanceName() + "@" + context.getNodeName());
        httpResponse.end();
    }
}
