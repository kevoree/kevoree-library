package org.kevoree.library;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.Output;
import org.kevoree.annotation.Param;
import org.kevoree.api.Port;
import org.kevoree.log.Log;

import java.io.IOException;

/**
 * Created by mleduc on 18/11/15.
 */
@ComponentType(description = "Return a list of tags from an image")
public class Imagga {

    @Param(optional = false)
    private String username;

    @Param(optional = false)
    private String password;

    @Param(optional = false, defaultValue = "false")
    private Boolean content;

    private final ImaggaService imaggaService = new ImaggaService();
    private final SerializerService serializerService = new SerializerService();

    @Output
    private Port tags;

    @Input
    public void in(final String payload) {
        try {
            final ImaggaTagSet res = imaggaService.query(username, password, payload, content);
            tags.send(serializerService.toJson(res), null);
        } catch (UnirestException e) {
            Log.error(e.getMessage());
        } catch (ImaggaException e) {
            Log.error(e.getMessage());
        } catch (IOException e) {
            Log.error(e.getMessage());
        }
    }


}
