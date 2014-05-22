package org.kevoree.library.cloud.docker.client;

/**
 * Created by leiko on 22/05/14.
 */
public class DockerException extends Exception {

    public DockerException() {}

    public DockerException(String message) {
        super(message);
    }

    public DockerException(String message, Throwable cause) {
        super(message, cause);
    }

    public DockerException(Throwable cause) {
        super(cause);
    }
}
