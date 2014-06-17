package org.kevoree.library.java.editor.service.load;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.kevoree.library.java.editor.model.Library;
import org.kevoree.library.java.editor.parser.XMLLibraryParser;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 20/01/14
 * Time: 11:56
 */
public class JavaLoadService implements LoadService {

    private String platform;

    public JavaLoadService(String platform) {
        if (platform != null && platform.length() > 0) {
            this.platform = platform;
        } else {
            this.platform = "java";
        }
    }

    @Override
    public JsonObject process() throws Exception {
        URL url = new URL("https://oss.sonatype.org/service/local/lucene/search?p=jar&g=org.kevoree.library."+this.platform);
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
        for (Library lib : libraries) {
            jsonLibraries.add(lib.toJsonObject());
        }
        jsonRes.add("libraries", jsonLibraries);

        return jsonRes;
    }

    public static void main(String[] args) throws Exception {
        URL url = new URL("https://oss.sonatype.org/service/local/lucene/search?p=jar&g=org.kevoree.library.cloud");
        URLConnection conn = url.openConnection();
        InputStream is = conn.getInputStream();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);
        is.close();
        NodeList nList = doc.getElementsByTagName("artifact");

        XMLLibraryParser libParser = new XMLLibraryParser(nList);
        Collection<Library> libraries = libParser.getLibraries();
        for (Library lib : libraries) {
            System.out.println(lib.toJsonObject().get("latest"));
        }
    }
}
