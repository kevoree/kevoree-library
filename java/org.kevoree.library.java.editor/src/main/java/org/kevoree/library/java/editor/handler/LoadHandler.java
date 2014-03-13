package org.kevoree.library.java.editor.handler;

import com.google.gson.JsonObject;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.kevoree.library.java.editor.service.ServiceCallback;
import org.kevoree.library.java.editor.service.load.JavaLoadService;
import org.kevoree.library.java.editor.service.load.LoadService;
import org.kevoree.library.java.editor.service.load.NpmLoadService;
import org.kevoree.log.Log;

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

    @Override
    public void handle(String s, final Request request, HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) throws IOException, ServletException {
        if (s.equals("/load")) {
            final String callback = request.getParameter("callback");
            final String platform = request.getParameter("platform");
            
            ServiceCallback serviceCallback = new ServiceCallback() {
                @Override
                public void onSuccess(JsonObject jsonRes) {
                    try {
                        String jsonpRes = callback + " && " + callback + "(" + jsonRes.toString() + ");";
                        httpServletResponse.setDateHeader("Content-Length", jsonpRes.length());
                        httpServletResponse.setContentType("text/javascript");
                        httpServletResponse.getWriter().write(jsonpRes);
                        request.setHandled(true);
                    } catch (Exception e) {
                        Log.error(e.getMessage());
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.error("ERROR /load {platform: "+platform+"}: " + e.getMessage());
                }
            };

            if (platform.equals("java") || platform.equals("cloud")) {
                JavaLoadService javaLoadService = new JavaLoadService(platform);
                javaLoadService.process(serviceCallback);
                
            } else if (platform.equals("javascript")) {
                NpmLoadService npmLoadService = new NpmLoadService();
                npmLoadService.process(serviceCallback);
            }
        }
    }
}
