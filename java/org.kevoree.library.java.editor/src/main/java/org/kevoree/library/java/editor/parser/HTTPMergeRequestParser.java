package org.kevoree.library.java.editor.parser;

import org.kevoree.library.java.editor.model.Library;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 22/01/14
 * Time: 17:40
 */
public class HTTPMergeRequestParser {

    private static final String PLATFORM       = "[a-z]+";
    private static final String PLATFORM_BASE  = "libz\\[("+PLATFORM+")\\].+";
    private static final String PLATFORM_ARTID = "libz\\["+PLATFORM+"\\]\\[[0-9]+\\]\\[artifactID\\]";
    private static final String PLATFORM_GRPID = "libz\\["+PLATFORM+"\\]\\[[0-9]+\\]\\[groupID\\]";
    private static final String PLATFORM_VERS  = "libz\\["+PLATFORM+"\\]\\[[0-9]+\\]\\[version\\]";
    private static final String PLATFORM_INDEX = "libz\\["+PLATFORM+"\\]\\[([0-9]+)\\].+";
    
    private Map<String, Map<String, Library>> data;
    
    public HTTPMergeRequestParser() {
        data = new HashMap<String, Map<String, Library>>();
    }
    
    public Map<String, Collection<Library>> parse(Set<Entry<String, String[]>> entries) {
        Pattern platformPattern = Pattern.compile(PLATFORM_BASE);
        
        for (Entry<String, String[]> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue()[0];
            
            Matcher matcher = platformPattern.matcher(key);
            if (matcher.find()) {
                String platform = matcher.group(1);
                processPlatform(platform, Pattern.compile(PLATFORM_INDEX.replace(PLATFORM, platform)), key, value);
            }
        }

        Map<String, Collection<Library>> result = new HashMap<String, Collection<Library>>();
        for (Entry<String, Map<String, Library>> entry : this.data.entrySet()) {
            result.put(entry.getKey(), entry.getValue().values());
        }
        return result;
    }
    
    private void processPlatform(String platform, Pattern idxPattern, String key, String value) {
        Map<String, Library> javaLibs = this.data.get(platform);
        if (javaLibs == null) {
            javaLibs = new HashMap<String, Library>();
            this.data.put(platform, javaLibs);
        }

        Matcher matcher = idxPattern.matcher(key);
        if (matcher.find()) {
            Library lib = javaLibs.get(matcher.group(1));
            if (lib == null) {
                lib = new Library();
                javaLibs.put(matcher.group(1), lib);
            }

            if (key.matches(PLATFORM_ARTID.replace(PLATFORM, platform))) {
                lib.setArtifactId(value);
            } else if (key.matches(PLATFORM_GRPID.replace(PLATFORM, platform))) {
                lib.setGroupId(value);
            } else if (key.matches(PLATFORM_VERS.replace(PLATFORM, platform))) {
                lib.addVersion(value);
            }
        }
    }

    public static void main(String[] args) {
        Set<Entry<String, String[]>> entries = new HashSet<Entry<String, String[]>>();
        entries.add(new AbstractMap.SimpleEntry<String, String[]>("libz[java][0][artifactID]", new String[] {"org.kevoree.library.java.javaNode"}));
        entries.add(new AbstractMap.SimpleEntry<String, String[]>("libz[java][1][groupID]", new String[] {"org.kevoree.library.java"}));
        entries.add(new AbstractMap.SimpleEntry<String, String[]>("libz[java][0][version]", new String[] {"3.1.5-SNAPSHOT"}));
        entries.add(new AbstractMap.SimpleEntry<String, String[]>("libz[java][1][artifactID]", new String[] {"tralala"}));
        entries.add(new AbstractMap.SimpleEntry<String, String[]>("libz[java][0][groupID]", new String[] {"youpi"}));
        entries.add(new AbstractMap.SimpleEntry<String, String[]>("libz[java][1][version]", new String[] {"3.1.5"}));
        entries.add(new AbstractMap.SimpleEntry<String, String[]>("libz[javascript][0][artifactID]", new String[] {"kevoree-group-websocket"}));
        entries.add(new AbstractMap.SimpleEntry<String, String[]>("libz[javascript][0][groupID]", new String[] {""}));
        entries.add(new AbstractMap.SimpleEntry<String, String[]>("libz[javascript][0][version]", new String[] {"1.2.3"}));
        
        
        HTTPMergeRequestParser parser = new HTTPMergeRequestParser();
        Map<String, Collection<Library>> data = parser.parse(entries);
        for (Entry<String, Collection<Library>> entry : data.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println("================");
            for (Library lib : entry.getValue()) {
                System.out.println(lib.toString());
            }
            System.out.println("================");
        }
    }
}
