package org.kevoree.library.java.editor.handler;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.kevoree.library.java.editor.model.Library;
import org.kevoree.library.java.editor.parser.XMLLibraryParser;
import org.kevoree.library.java.editor.parser.XMLLibraryParser;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 17/01/14
 * Time: 11:28
 */
public class LoadHandler extends AbstractHandler {
    
    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        if (s.equals("/load")) {
            String callback = request.getParameter("callback");
            System.out.println("Callback: "+callback);
            String platform = request.getParameter("platform");
            System.out.println("Platform: "+platform);
            
            if (platform.equals("java")) {
                try {
                    URL url = new URL("http://oss.sonatype.org/service/local/data_index?g=org.kevoree.library."+platform);
                    URLConnection conn = url.openConnection();
                    InputStream is = conn.getInputStream();
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(is);
                    is.close();
                    NodeList nList = doc.getElementsByTagName("artifact");
                    
                    XMLLibraryParser libParser = new XMLLibraryParser(nList);
                    Collection<Library> libraries = libParser.getLibraries();
                    
                    // JSON Response object
                    JsonObject jsonRes = new JsonObject();
                    jsonRes.add("result", new JsonPrimitive(1));
                    jsonRes.add("message", new JsonPrimitive("Ok"));
                    JsonArray jsonLibraries = new JsonArray();
                    for (Library lib : libraries) jsonLibraries.add(lib.toJsonObject()); 
                    jsonRes.add("libraries", jsonLibraries);
                    
                    request.setHandled(true);
                    String jsonpRes = callback+" && "+callback+"("+jsonRes.toString()+");";
                    httpServletResponse.setDateHeader("Content-Length", jsonpRes.length());
                    httpServletResponse.setContentType("text/javascript");
                    httpServletResponse.getWriter().write(jsonpRes);

                } catch (Exception e) {
                    System.err.println("LoadHandler ERROR: "+e.getMessage());
                }
            }
        }
    }
}
