//package org.kevoree.library.web.jetty;
//
//import org.eclipse.jetty.server.Request;
//import org.eclipse.jetty.server.Server;
//import org.eclipse.jetty.server.handler.AbstractHandler;
//import org.kevoree.annotation.ComponentType;
//import org.kevoree.annotation.Param;
//import org.kevoree.annotation.Start;
//import org.kevoree.annotation.Stop;
//
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//
///**
// * Created by duke on 05/12/2013.
// */
////@ComponentType
//public class BlogServer extends AbstractHandler {
//
//    @Param(defaultValue = "8080",optional = true)
//    Integer port;
//
//    private Server server = null;
//
//    @Start
//    public void startBlogServer() throws Exception {
//        server = new Server(port);
//        server.setHandler(this);
//        server.start();
//    }
//
//    @Stop
//    public void stopBlogServer() throws Exception {
//        server.stop();
//    }
//
//
//    @Override
//    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
//        System.out.println("Hello");
//    }
//}
