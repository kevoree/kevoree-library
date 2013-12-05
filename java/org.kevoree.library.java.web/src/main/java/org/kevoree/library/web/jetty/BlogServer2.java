package org.kevoree.library.web.jetty;

import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.webbitserver.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;

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

    String pageCache = "";
    String[] colors = {"#e67e22", "#8e44ad", "#1abc9c", "#3498db", "#2c3e50"};

    @Start
    public void start() {
        pageCache = read(this.getClass().getClassLoader().getResourceAsStream("hello.html"));
        int randomColorIndice = new Random().nextInt(colors.length);
        pageCache = pageCache.replace("${background.color}", colors[randomColorIndice]);
        pageCache = pageCache.replace("${name}", context.getInstanceName());
        pageCache = pageCache.replace("${node.name}", context.getNodeName());
        webServer = WebServers.createWebServer(http_port);
        webServer.add(this);
        webServer.start();
    }

    @Stop
    public void stop() throws InterruptedException {
        webServer.stop();
        Thread.sleep(2000);
    }

    @Override
    public void handleHttpRequest(HttpRequest httpRequest, HttpResponse httpResponse, HttpControl httpControl) throws Exception {
        httpResponse.content(pageCache);
        httpResponse.end();
    }

    public static String read(InputStream in) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
            }
            br.close();
            return buffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
