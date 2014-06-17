package org.kevoree.library.java.editor.service.load;

import com.google.gson.*;
import fr.braindead.npmjava.command.NpmSearch;
import fr.braindead.npmjava.command.NpmView;
import org.kevoree.library.java.editor.model.Library;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 20/01/14
 * Time: 11:56
 */
public class NpmLoadService implements LoadService {
    
    private static final String KEVOREE_PATTERN = "^kevoree-(chan|node|group|comp)-[\\w-]+";
    
    @Override
    public JsonObject process() throws Exception {
        NpmSearch npmSearch = new NpmSearch();

        JsonArray searchRes = npmSearch.execute(KEVOREE_PATTERN);
        Map<String, Library> libMap = new HashMap<String, Library>();
        NpmView npmView = new NpmView();

        for (int i=0; i < searchRes.size(); i++) {
            JsonObject jsonLib = (JsonObject) searchRes.get(i);
            if (jsonLib.get("versions") != null) {
                JsonArray keywords = jsonLib.getAsJsonArray("keywords");
                if (keywords != null && keywords.size() > 0) {
                    boolean isStdLib = false;
                    for (int j=0; j < keywords.size(); j++) {
                        if (keywords.get(j).getAsString().equals("kevoree-std-lib")) {
                            isStdLib = true;
                            break;
                        }
                    }
                    if (isStdLib) {
                        String artId = jsonLib.get("name").getAsString();
                        JsonObject libView = npmView.execute(artId);
                        Library lib = libMap.get(artId);
                        if (lib == null) {
                            lib = new Library();
                            lib.setGroupId("");
                            lib.setArtifactId(artId);
                            lib.setLatestRelease(libView.get("version").getAsString());
                            String[] splittedArtId = artId.split("-", 3);
                            lib.setSimpleName(splittedArtId[splittedArtId.length-1]);
                            lib.setType(splittedArtId[1]);
                            libMap.put(artId, lib);
                            JsonArray versions = libView.getAsJsonArray("versions");
                            for (JsonElement vers : versions) {
                                lib.addVersion(vers.getAsString());
                            }
                        }
                    }
                }
            }
        }

        JsonObject res = new JsonObject();
        res.add("result", new JsonPrimitive(1));
        res.add("message", new JsonPrimitive("Ok"));
        JsonArray jsonLibraries = new JsonArray();
        for (Library lib : libMap.values()) jsonLibraries.add(lib.toJsonObject());
        res.add("libraries", jsonLibraries);

        return res;
    }

    public static void main(String[] args) throws Exception {
        NpmLoadService service = new NpmLoadService();
        JsonObject obj = service.process();
        System.out.println("LOADED");
        System.out.println(obj.toString());
    }
}
