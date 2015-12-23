package org.kevoree.library;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mleduc on 18/11/15.
 */
public class ImaggaService {
    private static final String HTTP_ENDPOINT = "http://api.imagga.com/v1/";
    public static final String HTTP_ENDPOINT_TAGGING = HTTP_ENDPOINT + "tagging";
    private static final String HTTP_ENDPOINT_CONTENT = HTTP_ENDPOINT + "content";

    public ImaggaTagSet query(final String username, final String password, final String payload, final Boolean content) throws UnirestException, ImaggaException, IOException {
        final HttpResponse<JsonNode> response;
        if (!content) {
            response = urlMode(username, password, payload);
        } else {
            response = contentMode(username, password, payload);
        }
        final JsonNode body = response.getBody();
        final JSONObject object = body.getObject();
        if (object.has("status") && object.get("status").equals("error")) {
            throw new ImaggaException(body.toString());
        } else if (object.has("unsuccessful")) {
            throw new ImaggaException(body.toString());
        }

        return parse(object, content, payload);
    }

    private HttpResponse<JsonNode> contentMode(final String username, final String password, final String b64Image) throws UnirestException, IOException {

        File tempFile = null;
        try {
            tempFile = b64ToFile(b64Image);
            final HttpResponse<JsonNode> content = uploadContent(username, password, tempFile);
            final String contentId = parseContentResponse(content);
            return queryTagsByContent(username, password, contentId);
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    private String parseContentResponse(HttpResponse<JsonNode> content) {
        return content.getBody().getObject().getJSONArray("uploaded").getJSONObject(0).getString("id");
    }

    private HttpResponse<JsonNode> queryTagsByContent(String username, String password, String contentId) throws UnirestException {
        return Unirest.get(HTTP_ENDPOINT_TAGGING + "?content={content}&version=2")
                .routeParam("content", contentId)
                .header("accept", "application/json")
                .basicAuth(username, password)
                .asJson();
    }

    private HttpResponse<JsonNode> uploadContent(String username, String password, File tempFile) throws UnirestException {
        return Unirest.post(HTTP_ENDPOINT_CONTENT)
                .basicAuth(username, password)
                .header("accept", "application/json")
                .field("image", tempFile)
                .asJson();
    }

    private File b64ToFile(String b64Image) throws IOException {
        File tempFile;
        tempFile = File.createTempFile("immaga", ".jpg");
        final FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        fileOutputStream.write(Base64.getDecoder().decode(b64Image));
        return tempFile;
    }

    private HttpResponse<JsonNode> urlMode(String username, String password, String url) throws UnirestException {
        return Unirest.get(HTTP_ENDPOINT_TAGGING + "?url={url}&version=2")
                .routeParam("url", url)
                .header("accept", "application/json")
                .basicAuth(username, password)
                .asJson();
    }

    private ImaggaTagSet parse(final JSONObject object, final Boolean content, String payload) {
        final JSONArray results = object.getJSONArray("results");
        final JSONObject result = results.getJSONObject(0);
        final JSONArray tagsJson = result.getJSONArray("tags");
        final Set<ImaggaTag> tags = new HashSet<ImaggaTag>();
        for (int i = 0; i < tagsJson.length(); i++) {
            final JSONObject tagJson = tagsJson.getJSONObject(i);
            final Double confidence = tagJson.getDouble("confidence");
            final String tag = tagJson.getString("tag");
            tags.add(new ImaggaTag(confidence, tag));
        }

        return new ImaggaTagSet(payload, content, tags);
    }
}
