package org.kevoree.library;

import org.kevoree.annotation.*;
import org.kevoree.api.ModelService;
import org.kevoree.log.Log;
import org.kevoree.sky.web.EmbedHandler;
import org.kevoree.sky.web.ModelServiceSocketHandler;
import org.webbitserver.*;

import java.nio.charset.Charset;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 09/07/13
 * Time: 11:52
 */
@ComponentType(version = 1)
public class WebFrontend implements HttpHandler {

    @Param
    Integer port = 8080;

    @KevoreeInject
    ModelService modelService;


    @Param(optional = true, defaultValue = "10")
    int maxRetry;

    @Param(optional = true, defaultValue = "1000")
    long delayWhenNothing;

    private WebServer webServer;
    private ModelServiceSocketHandler mhandler = null;

    @Start
    public void startServer() {
        try {
            mhandler = new ModelServiceSocketHandler(modelService);
            webServer = WebServers.createWebServer(port)
                    .add(this)
                    .add("/model/service", mhandler)
                    .add(new EmbedHandler()) // path to web content
                    .start()
                    .get();
            Log.info("Cloud Web Interface started on http://localhost:{}", port);
        } catch (Exception e) {
            Log.error("Error while starting Kloud Web front end", e);
        }
    }

    @Stop
    public void stopServer() {
        webServer.stop();
        mhandler.destroy();
    }

    @Override
    public void handleHttpRequest(HttpRequest httpRequest, HttpResponse httpResponse, HttpControl httpControl) throws Exception {
        if (httpRequest.uri().equals("/metadata/nodeName")) {
            httpResponse.charset(Charset.forName("UTF-8"));
            httpResponse.content(modelService.getNodeName());
            httpResponse.end();
            return;
        }
        httpControl.nextHandler();
    }

}
