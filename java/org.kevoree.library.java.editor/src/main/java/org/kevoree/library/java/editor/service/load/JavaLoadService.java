package org.kevoree.library.java.editor.service.load;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.kevoree.library.java.editor.model.Library;
import org.kevoree.library.java.editor.parser.XMLLibraryParser;
import org.kevoree.library.java.editor.service.ServiceCallback;
import org.kevoree.library.java.editor.service.load.LoadService;
import org.kevoree.library.java.editor.service.load.LoadService;
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

    @Override
    public void process(ServiceCallback cb) {
        try {
            URL url = new URL("http://oss.sonatype.org/service/local/data_index?g=org.kevoree.library.java");
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
            
            cb.onSuccess(jsonRes);
            
        } catch (Exception e) {
            cb.onError(e);
        }
    }
}
