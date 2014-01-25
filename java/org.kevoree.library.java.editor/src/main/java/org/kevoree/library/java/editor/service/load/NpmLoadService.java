package org.kevoree.library.java.editor.service.load;

import com.google.gson.*;
import fr.braindead.npmjava.command.NpmSearch;
import fr.braindead.npmjava.command.NpmView;
import org.kevoree.library.java.editor.model.Library;
import org.kevoree.library.java.editor.service.ServiceCallback;
import org.kevoree.library.java.editor.service.load.LoadService;
import org.kevoree.library.java.editor.service.load.LoadService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 20/01/14
 * Time: 11:56
 */
public class NpmLoadService implements LoadService {
    
    private static final String KEVOREE_PATTERN = "(^kevoree-chan-|^kevoree-node-|^kevoree-group-|^kevoree-comp-).*";
    
    @Override
    public void process(final ServiceCallback cb) {
        NpmSearch npmSearch = new NpmSearch();
        npmSearch.execute(KEVOREE_PATTERN, new NpmSearch.SearchCallback() {
            @Override
            public void onSuccess(JsonArray searchRes) {
                Map<String, Library> libMap = new HashMap<String, Library>();
                NpmView npmView = new NpmView();
                
                for (int i=0; i < searchRes.size(); i++) {
                    JsonObject jsonLib = (JsonObject) searchRes.get(i);
                    String artId = jsonLib.get("name").getAsString();
                    try {
                        JsonObject libView = npmView.execute(artId);
                        Library lib = libMap.get(artId);
                        if (lib == null) {
                            lib = new Library();
                            lib.setGroupId("");
                            lib.setArtifactId(artId);
                            String[] splittedArtId = artId.split("-", 3);
                            lib.setSimpleName(splittedArtId[splittedArtId.length-1]);
                            lib.setType(splittedArtId[1]);
                            libMap.put(artId, lib);
                            JsonArray versions = libView.getAsJsonArray("versions");
                            for (JsonElement vers : versions) {
                                lib.addVersion(vers.getAsString());
                            }
                        }
                        
                    } catch (Exception e) {
                        cb.onError(e);
                    }
                }
                
                JsonObject res = new JsonObject();
                res.add("result", new JsonPrimitive(1));
                res.add("message", new JsonPrimitive("Ok"));
                JsonArray jsonLibraries = new JsonArray();
                for (Library lib : libMap.values()) jsonLibraries.add(lib.toJsonObject());
                res.add("libraries", jsonLibraries);
                cb.onSuccess(res);
            }

            @Override
            public void onError(Exception e) {
                cb.onError(e);
            }
        });
    }
    
    public static void main(String[] args) throws IOException {
        String test = "kevoree-node-javascript";
        String test1 = "kevoree-chan-websocket-super-cool";
        
        String[] res = test1.split("-", 3);
        for (String s : res) {
            System.out.println(s);
        }
    }
}