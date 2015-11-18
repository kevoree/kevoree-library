package org.kevoree.library;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.Output;
import org.kevoree.annotation.Param;
import org.kevoree.api.Port;
import org.kevoree.log.Log;

import java.lang.reflect.Modifier;

/**
 * Created by mleduc on 18/11/15.
 */
@ComponentType
public class Imagga {

    @Param(optional = false)
    private String username;

    @Param(optional = false)
    private String password;

    private final ImaggaService imaggaService = new ImaggaService();

    @Output
    private Port tags;

    @Input
    public void in(final String url) {
        try {
            final ImaggaTagSet res = imaggaService.query(username, password, url);
            tags.send(toJson(res), null);
        } catch (UnirestException e) {
            Log.error(e.getMessage());
        } catch (ImaggaException e) {
            Log.error(e.getMessage());
        }
    }

    private String toJson(final ImaggaTagSet imaggaTagSet) {
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                .serializeNulls()
                .create();
        return gson.toJson(imaggaTagSet).toString();
    }
}
