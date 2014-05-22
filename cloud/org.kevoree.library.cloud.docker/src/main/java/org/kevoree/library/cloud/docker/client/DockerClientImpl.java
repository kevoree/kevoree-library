package org.kevoree.library.cloud.docker.client;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.kevoree.library.cloud.docker.model.*;
import org.kevoree.log.Log;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by leiko on 22/05/14.
 */
public class DockerClientImpl implements DockerClient {

    private Client client;
    private String url;

    public DockerClientImpl(String url) {
        this.url = url;
        this.client = ClientBuilder
                .newBuilder()
                .register(JacksonJsonProvider.class)
                .build();
    }

    @Override
    public void start(String id) throws DockerException {
        processResponse(buildResponse(String.format(DockerApi.START_CONTAINER, id)), "Start", id);
    }

    @Override
    public void start(String id, HostConfig conf) throws DockerException {
        processResponse(buildResponse(String.format(DockerApi.START_CONTAINER, id), conf), "Start", id);
    }

    @Override
    public void stop(String id) throws DockerException {
        processResponse(buildResponse(String.format(DockerApi.STOP_CONTAINER, id)), "Stop", id);
    }

    @Override
    public void deleteContainer(String id) throws DockerException {
        Response response = this.client
                .target(this.url)
                .path(String.format(DockerApi.DELETE_CONTAINER, id))
                .queryParam("v", "")
                .queryParam("force", "")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();

        switch (response.getStatus()) {
            case 204:
                Log.info(String.format("Container %s successfully deleted", id));
                break;
            case 400:
                Log.error(String.format("Cannot delete %s - bad parameter", id));
                throw new DockerException(String.format("Cannot delete %s - bad parameter", id));
            case 404:
                Log.error(String.format("Container %s not found", id));
                throw new DockerException(String.format("No such container %s", id));
            case 500:
                Log.error("Docker Server Error");
                throw new DockerException("Docker Server Error");
            default:
                throw new DockerException(String.format("Unknown Error: %s", response.getStatusInfo().getReasonPhrase()));
        }
    }

    @Override
    public ContainerInfo commit(CommitConfig conf) throws DockerException {
        Response response = this.client
                .target(this.url)
                .path(DockerApi.COMMIT_IMAGE)
                .queryParam("container", conf.getContainer())
                .queryParam("m", conf.getMessage())
                .queryParam("repo", conf.getRepo())
                .queryParam("tag", conf.getTag())
                .queryParam("author", conf.getAuthor())
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);


        switch (response.getStatus()) {
            case 201:
                ContainerInfo res = response.readEntity(ContainerInfo.class);
                Log.info(String.format("Commit container %s to %s: success", conf.getContainer(), res.getId()));
                return res;
            case 404:
                Log.error(String.format("Commit container %s: failed (not found)", conf.getContainer()));
                throw new DockerException(String.format("Commit failed - container %s not found", conf.getContainer()));
            case 500:
                Log.error("Docker Server Error");
                throw new DockerException("Docker Server Error");
            default:
                Log.error(response.getStatusInfo().getReasonPhrase());
                throw new DockerException(String.format("Unknown Error: %s", response.getStatusInfo().getReasonPhrase()));
        }
    }

    @Override
    public ImageDetail pull(String name) throws DockerException {
        return this.pull(name, null);
    }

    @Override
    public ImageDetail pull(String name, String tag) throws DockerException {
        Response response = this.client
                .target(this.url)
                .path(DockerApi.CREATE_IMAGE)
                .queryParam("fromImage", name)
                .queryParam("tag", tag)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(null);

        switch (response.getStatus()) {
            case 200:
                ImageDetail res = response.readEntity(ImageDetail.class);
                Log.info(String.format("Image %s pulled successfully", (tag == null) ? name : String.format("%s:%s", name, tag)));
                return res;
            case 500:
                Log.error("Docker Server Error");
                throw new DockerException("Docker Server Error");
            default:
                throw new DockerException(String.format("Unknown Error: %s", response.getStatusInfo().getReasonPhrase()));
        }
    }

    @Override
    public ImageDetail createImage(ImageConfig conf) throws DockerException {
        Response response = this.client
                .target(this.url)
                .path(DockerApi.CREATE_IMAGE)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(conf));

        switch (response.getStatus()) {
            case 200:
                Log.info("Image successfully created");
                break;
            case 500:
                Log.error("Docker Server Error");
                throw new DockerException("Docker Server Error");
            default:
                throw new DockerException(String.format("Unknown Error: %s", response.getStatusInfo().getReasonPhrase()));
        }

        return response.readEntity(ImageDetail.class);
    }

    @Override
    public ContainerInfo createContainer(ContainerConfig conf) throws DockerException {
        return this.createContainer(conf, null);
    }

    @Override
    public ContainerInfo createContainer(ContainerConfig conf, String name) throws DockerException {
        if (name != null && name.length() > 0 && !name.matches("/?[a-zA-Z0-9_-]+")) {
            throw new DockerException(String.format("Container name must match /?[a-zA-Z0-9_-]+ but '%s' does not", name));
        }

        Response response = this.client
                .target(this.url)
                .path(DockerApi.CREATE_CONTAINER)
                .queryParam("name", name)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(conf));

        switch (response.getStatus()) {
            case 201:
                Log.info("Container successfully created");
                break;
            case 404:
                Log.error("Container not found");
                throw new DockerException("No such container");
            case 406:
                Log.error("Unable to attach to container, container not running");
                throw new DockerException("Unable to attach to container");
            case 500:
                Log.error("Docker Server Error");
                throw new DockerException("Docker Server Error");
            default:
                throw new DockerException(String.format("Unknown Error: %s", response.getStatusInfo().getReasonPhrase()));
        }

        return response.readEntity(ContainerInfo.class);
    }

    private Response buildResponse(String path) {
        return this.buildResponse(path, null);
    }

    private Response buildResponse(String path, Object obj) {
        if (obj == null) {
            return this.client.target(this.url).path(path).request(MediaType.APPLICATION_JSON_TYPE).post(null);
        } else {
            return this.client.target(this.url).path(path).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(obj));
        }
    }

    private void processResponse(Response response, String action, String id) throws DockerException {
        switch (response.getStatus()) {
            case 204:
                Log.info(String.format("%s container %s: success", action, id));
                break;
            case 404:
                Log.error(String.format("%s container %s: failed (not found)", action, id));
                throw new DockerException(String.format("%s failed - container %s not found", action, id));
            case 500:
                Log.error("Docker Server Error");
                throw new DockerException("Docker Server Error");
            default:
                throw new DockerException(String.format("Unknown Error: %s", response.getStatusInfo().getReasonPhrase()));
        }
    }
}
