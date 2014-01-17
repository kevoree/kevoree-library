package org.kevoree.library.java.editor.handler;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;


/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 25/11/13
 * Time: 17:46
 */
public class ClasspathResourceHandler extends AbstractHandler {

    private final MimeTypes _mimeTypes = new MimeTypes();

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        String requestURI = s;
        if(requestURI.equals("/")||requestURI.equals("")||requestURI.equals("/index.html")){
           requestURI = "/editor.html";
        }
        InputStream res = this.getClass().getResourceAsStream(requestURI);
        if (res != null) {
            request.setHandled(true);
            httpServletResponse.setDateHeader("Content-Length", res.available());
            httpServletResponse.setContentType(_mimeTypes.getMimeByExtension(s));
            while (res.available() > 0) {
                httpServletResponse.getWriter().write(res.read());
            }
        }
    }
}
