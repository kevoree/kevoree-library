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
    private String latestRelease;
    private String latestSnapshot;

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

    public void setLatestRelease(String version) {
        this.latestRelease = version;
    }

    public void setLatestSnapshot(String version) {
        this.latestSnapshot = version;
    }

    public void addVersion(String version) {
        if (!this.versions.contains(version)) {
            // do not duplicate versions
            this.versions.add(version);
        }
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

    public String getLatestRelease() {
        return this.latestRelease;
    }

    public String getLatestSnapshot() {
        return this.latestSnapshot;
    }

    public JsonObject toJsonObject() {
        JsonObject obj = new JsonObject();
        if (groupId != null) {
            obj.add("groupID", new JsonPrimitive(groupId));
        }
        obj.add("artifactID", new JsonPrimitive(artifactId));
        obj.add("simpleName", new JsonPrimitive(simpleName));
        if (type != null) {
            obj.add("type", new JsonPrimitive(type));
        }
        if (latestRelease == null) {
            obj.add("latest", new JsonPrimitive(latestSnapshot));
        } else {
            obj.add("latest", new JsonPrimitive(latestRelease));
        }
        JsonArray jsonVersions = new JsonArray();
        for (String version : versions) {
            jsonVersions.add(new JsonPrimitive(version));
        }
        obj.add("versions", jsonVersions);
        return obj;
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
