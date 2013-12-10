package com.kpelykh.docker.client.model;


import org.codehaus.jackson.annotate.JsonProperty;

import java.util.Arrays;
import java.util.Map;

/**
 *
 * @author Konstantin Pelykh (kpelykh@gmail.com)
 *
 */
public class ContainerInspectResponse {

    @JsonProperty("ID") public String id;
    @JsonProperty("Created") public String created;
    @JsonProperty("Path") public String path;
    @JsonProperty("Args") public String[] args;
    @JsonProperty("Config") public ContainerConfig config;
    @JsonProperty("State") public ContainerState state;
    @JsonProperty("Image") public String image;
    @JsonProperty("NetworkSettings") public NetworkSettings networkSettings;
    @JsonProperty("SysInitPath") public String sysInitPath;
    @JsonProperty("ResolvConfPath") public String resolvConfPath;
    @JsonProperty("Volumes") public Map<String, String> volumes;
    @JsonProperty("VolumesRW") public Map<String, String> volumesRW;

    @Override
    public String toString() {
        return "ContainerInspectResponse{" +
                "volumes=" + volumes +
                ", volumesRW=" + volumesRW + '\'' +
                ", resolvConfPath='" + resolvConfPath + '\'' +
                ", sysInitPath='" + sysInitPath + '\'' +
                ", id='" + id + '\'' +
                ", created='" + created + '\'' +
                ", path='" + path + '\'' +
                ", args=" + Arrays.toString(args) +
                ", config=" + config +
                ", state=" + state +
                ", image='" + image + '\'' +
                ", networkSettings=" + networkSettings +
                '}';
    }

    public class NetworkSettings {

        @JsonProperty("IPAddress") public String ipAddress;
        @JsonProperty("IPPrefixLen") public int ipPrefixLen;
        @JsonProperty("Gateway") public String gateway;
        @JsonProperty("Bridge") public String bridge;
        @JsonProperty("PortMapping") public Map<String,Map<String, String>> portMapping;
    }

    public class ContainerState {

        @JsonProperty("Running") public boolean running;
        @JsonProperty("Pid") public int pid;
        @JsonProperty("ExitCode") public int exitCode;
        @JsonProperty("StartedAt") public String startedAt;
        @JsonProperty("Ghost") public boolean ghost;

        @Override
        public String toString() {
            return "ContainerState{" +
                    "running=" + running +
                    ", pid=" + pid +
                    ", exitCode=" + exitCode +
                    ", startedAt='" + startedAt + '\'' +
                    ", ghost=" + ghost +
                    '}';
        }


    }

}
