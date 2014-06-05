package org.kevoree.library.java.editor.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.kevoree.library.java.editor.cache.CacheHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 17/01/14
 * Time: 11:28
 */
public class LoadHandler extends AbstractHandler {

    private CacheHandler cacheHandler;

    public LoadHandler(CacheHandler cacheHandler) {
        this.cacheHandler = cacheHandler;
    }

    @Override
    public void handle(String s, final Request request, HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) throws IOException, ServletException {
        if (s.equals("/load")) {
            final String callback = request.getParameter("callback");
            final String platform = request.getParameter("platform");
            
            if (platform != null) {
                JsonObject jsonRes;
                int status = 200;
                if (platform.equals("java")) {
                    jsonRes = this.cacheHandler.getJavaLibs();

                } else if (platform.equals("cloud")) {
                    jsonRes = this.cacheHandler.getCloudLibs();

                } else if (platform.equals("javascript")) {
                    jsonRes = this.cacheHandler.getJSLibs();

                } else {
                    jsonRes = new JsonObject();
                    jsonRes.add("error", new JsonPrimitive("'"+platform+"' platform unknown"));
                    status = 204;
                }

                String jsonpRes = callback + " && " + callback + "(" + jsonRes.toString() + ");";
                httpServletResponse.setDateHeader("Content-Length", jsonpRes.length());
                httpServletResponse.setStatus(status);
                httpServletResponse.setContentType("text/javascript");
                httpServletResponse.getWriter().write(jsonpRes);
                request.setHandled(true);

            } else {
                httpServletResponse.setStatus(500);
                httpServletResponse.setContentType("application/json");
                JsonObject res = new JsonObject();
                res.add("error", new JsonPrimitive("No 'platform' parameter given"));
                httpServletResponse.getWriter().write(res.toString());
                request.setHandled(true);
            }
        }
    }
}
