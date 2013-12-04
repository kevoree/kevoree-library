package org.kevoree.library.java.hazelcast.message;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by duke on 04/12/2013.
 */
public class Response implements Serializable {
    private UUID id;
    Object payload;

    public UUID getId() {
        return id;
    }

    public Object getPayload() {
        return payload;
    }

    public Response(UUID id, Object obj) {
        this.payload = obj;
        this.id = id;
    }
}