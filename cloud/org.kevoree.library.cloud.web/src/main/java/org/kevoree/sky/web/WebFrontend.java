package org.kevoree.sky.web;

import org.kevoree.annotation.*;
import org.kevoree.api.ModelService;
import org.kevoree.log.Log;
import org.webbitserver.WebServer;
import org.webbitserver.WebServers;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 09/07/13
 * Time: 11:52
 */
@ComponentType
public class WebFrontend {

    @Param
    Integer port = 8080;

    @KevoreeInject
    ModelService modelService;

    private WebServer webServer;
    private ModelServiceSocketHandler mhandler = null;

    @Start
    public void startServer() {
        try {
            mhandler = new ModelServiceSocketHandler(modelService);
            webServer = WebServers.createWebServer(port)
                    .add(new MetaDataHandler(modelService))
                    .add("/model/service", mhandler)
                    .add(new EmbedHandler()) // path to web content
                    .start()
                    .get();
        } catch (Exception e) {
            Log.error("Error while starting Kloud Web front end", e);
        }
    }

    @Stop
    public void stopServer() {
        webServer.stop();
        mhandler.destroy();
    }

}
