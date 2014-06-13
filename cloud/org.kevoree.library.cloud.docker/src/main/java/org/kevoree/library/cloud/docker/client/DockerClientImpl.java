package org.kevoree.library.cloud.docker.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.IOUtils;
import org.kevoree.library.cloud.docker.model.*;
import org.kevoree.log.Log;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.web.Content;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.List;

import static us.monoid.web.Resty.*;

/**
 * Created by leiko on 22/05/14.
 */
public class DockerClientImpl implements DockerClient {

    private static final int KILL_TIMEOUT = 15;

    private Resty resty;
    private String url;

    public DockerClientImpl(String url) {
        this.resty = new Resty();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 2);
        }
        this.url = url;
    }

    @Override
    public void start(String id) throws DockerException, JSONException {
        this.start(id, null);
    }

    @Override
    public void start(String id, HostConfig conf) throws DockerException, JSONException {
        try {
            ObjectMapper mapper = build();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(baos);
            mapper.writeValue(osw, conf);

            this.resty.json(
                    this.url + String.format(DockerApi.START_CONTAINER, id),
                    new Content("application/json; charset=UTF-8", baos.toByteArray())
            );
        } catch (IOException e) {
            throw new DockerException(e.getMessage());
        }
    }

    @Override
    public void stop(String id) throws DockerException {
        this.stop(id, KILL_TIMEOUT);
    }

    @Override
    public void stop(String id, int timeout) throws DockerException {
        try {
            this.resty.text(this.url + String.format(DockerApi.STOP_CONTAINER, id), form(String.format("t=%d", timeout)));
        } catch (IOException e) {
            throw new DockerException(e.getMessage());
        }
    }

    @Override
    public List<Container> getContainers() throws DockerException, JSONException {
        try {
            JSONArray res = this.resty.json(this.url + DockerApi.CONTAINERS_LIST).array();

            ObjectMapper mapper = build();
            return mapper.readValue(res.toString(), mapper.getTypeFactory().constructCollectionType(List.class, Container.class));

        } catch (IOException e) {
            throw new DockerException(e.getMessage());
        }
    }

    @Override
    public ContainerDetail getContainer(String idOrName) throws DockerException, JSONException {
        try {
            JSONResource res = this.resty.json(this.url + String.format(DockerApi.INSPECT_CONTAINER, idOrName));

            ObjectMapper mapper = build();
            return mapper.readValue(res.toObject().toString(), ContainerDetail.class);

        } catch (IOException e) {
            throw new DockerException(e.getMessage());
        }
    }

    @Override
    public void deleteContainer(String id) throws DockerException, JSONException {
        try {
            this.resty.json(this.url + String.format(DockerApi.DELETE_CONTAINER, id) + String.format("?force=%s", true), delete());
        } catch (IOException e) {
            throw new DockerException(e.getMessage());
        }
    }

    @Override
    public ContainerInfo commit(CommitConfig conf) throws DockerException, JSONException {
        try {
            JSONResource res = this.resty.json(
                    this.url + DockerApi.COMMIT_IMAGE,
                    form(String.format("container=%s&m=%s&repo=%s&tag=%s&author=%s", conf.getContainer(), conf.getMessage(), conf.getRepo(), conf.getTag(), conf.getAuthor()))
            );

            ObjectMapper mapper = build();
            return mapper.readValue(res.toObject().toString(), ContainerInfo.class);

        } catch (IOException e) {
            throw new DockerException(e.getMessage());
        }
    }

    @Override
    public void pull(String name) throws DockerException, JSONException {
        this.pull(name, "");
    }

    @Override
    public void pull(String name, String tag) throws DockerException, JSONException {
        if (tag == null) {
            tag = "";
        }
        try {
            Log.info("Pulling {}...(this may take a while)", name);
            JSONResource res = this.resty.json(
                    this.url + DockerApi.CREATE_IMAGE,
                    form(String.format("fromImage=%s&tag=%s", name, tag))
            );

            StringWriter writer = new StringWriter();
            IOUtils.copy(res.getUrlConnection().getInputStream(), writer, "UTF-8");
            Log.info("{} pulled successfully", name);

        } catch (IOException e) {
            throw new DockerException(e.getMessage());
        }
    }

    @Override
    public void createImage(ImageConfig conf) throws DockerException, JSONException {
        try {
            ObjectMapper mapper = build();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(baos);
            mapper.writeValue(osw, conf);

            JSONResource res = this.resty.json(this.url + DockerApi.CREATE_IMAGE, content(baos.toByteArray()));

            StringWriter writer = new StringWriter();
            IOUtils.copy(res.getUrlConnection().getInputStream(), writer, "UTF-8");

        } catch (IOException e) {
            throw new DockerException(e.getMessage());
        }
    }

    @Override
    public ContainerInfo createContainer(ContainerConfig conf) throws DockerException, JSONException {
        return this.createContainer(conf, "");
    }

    @Override
    public ContainerInfo createContainer(ContainerConfig conf, String name) throws DockerException, JSONException {
        if (name != null && name.length() > 0 && !name.matches("/?[a-zA-Z0-9_-]+")) {
            throw new DockerException(String.format("Container name must match /?[a-zA-Z0-9_-]+ but '%s' does not", name));
        }

        try {
            ObjectMapper mapper = build();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(baos);
            mapper.writeValue(osw, conf);

            JSONResource res = this.resty.json(this.url + DockerApi.CREATE_CONTAINER + String.format("?name=%s", name), content(baos.toByteArray()));

            return mapper.readValue(res.toObject().toString(), ContainerInfo.class);

        } catch (IOException e) {
            throw new DockerException(e.getMessage());
        }
    }

    public ObjectMapper build() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        return mapper;
    }

}
