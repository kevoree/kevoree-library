package org.kevoree.library.cloud.docker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author expi
 */
public class Port {
    @JsonProperty("PrivatePort")
    public int privatePort;

    @JsonProperty("PublicPort")
    public int publicPort;

    @JsonProperty("Type")
    public String type;

    @JsonProperty("IP")
    public String ip;

    @Override
    public String toString() {
        return "Port{" + "privatePort=" + privatePort + ", publicPort=" + publicPort + ", type=" + type + ", ip=" + ip + '}';
    }
}
