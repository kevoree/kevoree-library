package org.kevoree.library;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by mleduc on 18/11/15.
 */
public class ImaggaService {
    public static final String HTTP_ENDPOINT = "http://api.imagga.com/v1/tagging";

    public ImaggaTagSet query(final String username, final String password, final String url) throws UnirestException, ImaggaException {
        final HttpResponse<JsonNode> response = Unirest.get(HTTP_ENDPOINT + "?url={url}&version=2")
                .routeParam("url", url)
                .header("accept", "application/json")
                .basicAuth(username, password)
                .asJson();
        final JsonNode body = response.getBody();
        final JSONObject object = body.getObject();
        if (object.has("status") && object.get("status").equals("error")) {
            throw new ImaggaException(body.toString());
        } else if (object.has("unsuccessful")) {
            throw new ImaggaException(body.toString());
        }

        return parse(object);
    }

    private ImaggaTagSet parse(JSONObject object) {
        final JSONArray results = object.getJSONArray("results");
        final JSONObject result = results.getJSONObject(0);
        final String image = result.getString("image");
        final JSONArray tagsJson = result.getJSONArray("tags");
        final Set<ImaggaTag> tags = new HashSet<>();
        for (int i = 0; i < tagsJson.length(); i++) {
            final JSONObject tagJson = tagsJson.getJSONObject(i);
            final Double confidence = tagJson.getDouble("confidence");
            final String tag = tagJson.getString("tag");
            tags.add(new ImaggaTag(confidence, tag));
        }

        return new ImaggaTagSet(image, tags);
    }
}
