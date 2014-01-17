package org.kevoree.library.java.editor.parser;

import org.kevoree.library.java.editor.model.Library;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 17/01/14
 * Time: 15:08
 */
public class XMLLibraryParser {
    
    private Map<String, Library> libMap;
    
    public XMLLibraryParser(NodeList list) {
        this.libMap = new HashMap<String, Library>();

        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            NodeList childNode = node.getChildNodes();
            String resourceURI = null;
            String groupId = null;
            String artifactId = null;
            String version = null;
            for (int j = 0; j < childNode.getLength(); j++) {
                Node nodeChild = childNode.item(j);
                if (nodeChild.getNodeName().endsWith("resourceURI")) {
                    resourceURI = nodeChild.getTextContent();
                }
                if (nodeChild.getNodeName().endsWith("groupId")) {
                    groupId = nodeChild.getTextContent();
                }
                if (nodeChild.getNodeName().endsWith("artifactId")) {
                    artifactId = nodeChild.getTextContent();
                }
                if (nodeChild.getNodeName().endsWith("version")) {
                    version = nodeChild.getTextContent();
                }
            }
            if (resourceURI != null && !resourceURI.contains("-source")) {
                Library lib = this.libMap.get(artifactId);
                if (lib == null) {
                    lib = new Library();
                    lib.setGroupId(groupId);
                    lib.setArtifactId(artifactId);
                    String[] splittedArtId = artifactId.split("\\.");
                    lib.setSimpleName(splittedArtId[splittedArtId.length-1]);
                    this.libMap.put(artifactId, lib);
                }
                lib.addVersion(version);
            }
        }
    }
    
    public Collection<Library> getLibraries() {
        return this.libMap.values();
    } 
}
