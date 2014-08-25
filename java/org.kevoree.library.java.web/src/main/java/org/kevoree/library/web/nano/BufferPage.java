package org.kevoree.library.web.nano;

import org.kevoree.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 06/12/2013
 * Time: 08:09
 */
@ComponentType
public class BufferPage extends NanoHTTPD {

    @Param(defaultValue = "8080")
    Integer http_port;

    @Start
    public void startBlog() throws IOException {
        this.myPort = http_port;
        setTempFileManagerFactory(new DefaultTempFileManagerFactory());
        setAsyncRunner(new DefaultAsyncRunner());
        start();
    }

    private static final int max = 2000;

    @Input
    public void input(Object in) {
        if (in != null) {
            contentCache = in.toString()+"<br />"+contentCache;
            if(contentCache.length() > max){
                contentCache = contentCache.substring(0,max);
            }
        }
    }

    @Stop
    public void stopBlog() {
        stop();
    }

    String contentCache = "";

    @Override
    public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
        return new Response("<html><script>function timedRefresh(timeoutPeriod) {setTimeout(\"location.reload(true);\",timeoutPeriod);}</script><body onload=\"JavaScript:timedRefresh(1000);\" style=\"background-color:#3498db;color:#fff;\">" + contentCache + "</body></html>");
    }

}