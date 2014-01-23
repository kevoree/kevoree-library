package org.kevoree.library.java.editor.model;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 17/01/14
 * Time: 15:05
 */
public class Library {

    private String groupId;
    private String artifactId;
    private String simpleName;
    private String type;

    public void setType(String type) {
        this.type = type;
    }

    private List<String> versions;
    
    public Library() {
        this.versions = new ArrayList<String>();
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setSimpleName(String simpleName) {
        this.simpleName = simpleName;
    }

    public void addVersion(String version) {
        this.versions.add(version);
    }

    public List<String> getVersions() {
        return versions;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public JsonObject toJsonObject() {
        JsonObject obj = new JsonObject();
        if (groupId != null) obj.add("groupID", new JsonPrimitive(groupId));
        obj.add("artifactID", new JsonPrimitive(artifactId));
        obj.add("simpleName", new JsonPrimitive(simpleName));
        if (type != null) obj.add("type", new JsonPrimitive(type));
        obj.add("latest", new JsonPrimitive(getLatest()));
        JsonArray jsonVersions = new JsonArray();
        for (String version : versions) jsonVersions.add(new JsonPrimitive(version));
        obj.add("versions", jsonVersions);
        return obj;
    }
    
    private String getLatest() {
        String latest = this.versions.get(0);
        for (int i=1; i < this.versions.size(); i++) {
            Version v1 = Version.valueOf(latest);
            Version v2 = Version.valueOf(this.versions.get(i));
            if (v2.greaterThan(v1)) latest = this.versions.get(i);
        }
        
        return latest;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(groupId)
                .append(", ")
                .append(artifactId)
                .append(", ")
                .append(versions);
        return builder.toString();
    }
}
