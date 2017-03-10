package org.kevoree.library;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Modifier;

/**
 * Created by mleduc on 22/12/15.
 */
public class SerializerService {
    public String toJson(final ImaggaTagSet imaggaTagSet) {
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
                .serializeNulls()
                .create();
        return gson.toJson(imaggaTagSet);
    }
}
