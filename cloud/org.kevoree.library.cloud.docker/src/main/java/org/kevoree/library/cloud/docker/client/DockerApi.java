package org.kevoree.library.cloud.docker.client;

/**
 * Created by leiko on 22/05/14.
 */
public interface DockerApi {
    static final String CONTAINERS_LIST     = "/containers/json";
    static final String INSPECT_CONTAINER   = "/containers/%s/json";
    static final String CREATE_CONTAINER    = "/containers/create";
    static final String START_CONTAINER     = "/containers/%s/start";
    static final String STOP_CONTAINER      = "/containers/%s/stop";
    static final String KILL_CONTAINER      = "/containers/%s/kill";
    static final String DELETE_CONTAINER    = "/containers/%s/delete";
    static final String RESTART_CONTAINER   = "/containers/%s/restart";
    static final String COMMIT_IMAGE        = "/commit";
    static final String CREATE_IMAGE        = "/images/create";
}
