package org.kevoree.library.java.hazelcast.message;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by duke on 04/12/2013.
 */
public class Request implements Serializable {
    private UUID id = UUID.randomUUID();
    Object payload;

    public UUID getId() {
        return id;
    }

    public Object getPayload() {
        return payload;
    }

    public Request(Object obj) {
        this.payload = obj;
    }
}