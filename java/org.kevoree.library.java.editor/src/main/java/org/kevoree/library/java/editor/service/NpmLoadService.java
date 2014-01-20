package org.kevoree.library.java.editor.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 20/01/14
 * Time: 11:56
 */
public class NpmLoadService implements Service {
    
    @Override
    public void process(ServiceCallback cb) {
        try {
            URL url = new URL("http://registry.npmjs.org/-/all/since?stale=update_after&startkey=0");
            URLConnection conn = url.openConnection();
            InputStream is = conn.getInputStream();

            JsonParser jsonParser = new JsonParser();
            JsonObject npmdb = (JsonObject) jsonParser.parse(new InputStreamReader(is));
            
            // TODO caching and stuff
            
            

        } catch (Exception e) {
            cb.onError(e);
        }
    }
}
