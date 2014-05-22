package org.kevoree.library.cloud.docker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by leiko on 22/05/14.
 */
public class HostConfig {

    @JsonProperty("Binds")
    private String[] binds;

    @JsonProperty("LxcConf")
    private Object lxcConf;

    @JsonProperty("PortBindings")
    private String[] portBindings;

    @JsonProperty("PublishAllPorts")
    private boolean publishAllPorts;

    @JsonProperty("Privileged")
    private boolean privileged;

    public String[] getBinds() {
        return binds;
    }

    public void setBinds(String[] binds) {
        this.binds = binds;
    }

    public Object getLxcConf() {
        return lxcConf;
    }

    public void setLxcConf(Object lxcConf) {
        this.lxcConf = lxcConf;
    }

    public String[] getPortBindings() {
        return portBindings;
    }

    public void setPortBindings(String[] portBindings) {
        this.portBindings = portBindings;
    }

    public boolean isPublishAllPorts() {
        return publishAllPorts;
    }

    public void setPublishAllPorts(boolean publishAllPorts) {
        this.publishAllPorts = publishAllPorts;
    }

    public boolean isPrivileged() {
        return privileged;
    }

    public void setPrivileged(boolean privileged) {
        this.privileged = privileged;
    }

    @Override
    public String toString() {
        return String.format("HostConfig { binds=%s, lxcConf=%s, portBindings=%s, publishAllPorts=%s, priviledged=%s }", binds, lxcConf, portBindings, publishAllPorts, privileged);
    }
}
