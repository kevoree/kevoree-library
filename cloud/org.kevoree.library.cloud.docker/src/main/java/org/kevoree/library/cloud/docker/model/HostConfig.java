package org.kevoree.library.cloud.docker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by leiko on 22/05/14.
 */
public class HostConfig {

    @JsonProperty("ContainerIDFile") private String     containerIDFile;
    @JsonProperty("Binds")           private String[]   binds;
    @JsonProperty("LxcConf")         private Object     lxcConf;
    @JsonProperty("PortBindings")    private String[]   portBindings;
    @JsonProperty("PublishAllPorts") private boolean    publishAllPorts;
    @JsonProperty("Privileged")      private boolean    privileged;
    @JsonProperty("Links")           private Object     links;
    @JsonProperty("Dns")             private Object     dns;
    @JsonProperty("DnsSearch")       private Object     dnsSearch;
    @JsonProperty("VolumesFrom")     private Object     volumesFrom;
    @JsonProperty("NetworkMode")     private String     networkMode = "";

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

    public String getContainerIDFile() {
        return containerIDFile;
    }

    public void setContainerIDFile(String containerIDFile) {
        this.containerIDFile = containerIDFile;
    }

    public Object getLinks() {
        return links;
    }

    public void setLinks(Object links) {
        this.links = links;
    }

    public Object getDns() {
        return dns;
    }

    public void setDns(Object dns) {
        this.dns = dns;
    }

    public Object getDnsSearch() {
        return dnsSearch;
    }

    public void setDnsSearch(Object dnsSearch) {
        this.dnsSearch = dnsSearch;
    }

    public Object getVolumesFrom() {
        return volumesFrom;
    }

    public void setVolumesFrom(Object volumesFrom) {
        this.volumesFrom = volumesFrom;
    }

    public String getNetworkMode() {
        return networkMode;
    }

    public void setNetworkMode(String networkMode) {
        this.networkMode = networkMode;
    }

    @Override
    public String toString() {
        return String.format("HostConfig { binds=%s, lxcConf=%s, portBindings=%s, publishAllPorts=%s, priviledged=%s }", binds, lxcConf, portBindings, publishAllPorts, privileged);
    }
}