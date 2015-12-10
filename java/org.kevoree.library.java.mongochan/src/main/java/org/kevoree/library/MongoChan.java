package org.kevoree.library;

import com.mongodb.ServerAddress;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.connection.ClusterSettings;
//import org.bson.*;
import org.bson.BsonString;
import org.bson.Document;
import org.kevoree.annotation.*;
import org.kevoree.api.Callback;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.*;

import javax.print.Doc;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.kevoree.api.*;
import org.kevoree.Channel;
import org.kevoree.ContainerRoot;

import org.kevoree.Channel;
import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.*;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.json.JSONModelLoader;
import org.kevoree.pmodeling.api.json.JSONModelSerializer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

/**
 * Created by mleduc on 25/11/15.
 */
@org.kevoree.annotation.ChannelType
public class MongoChan implements ChannelDispatch {

    @Param(defaultValue = "localhost", optional = false)
    private String host;

    @Param(defaultValue = "27017", optional = false)
    private int port;

    @Param(optional = false)
    private String database;

    @Param(optional = false)
    private String collection;

    private MongoDatabase db;
    private MongoClient mongoClient;

    @KevoreeInject
    ChannelContext channelContext;

    @KevoreeInject
    private ModelService modelService;

    @KevoreeInject
    Context context;

    private ScheduledExecutorService service;

    @Start
    public void start() {
        final ClusterSettings clusterSettings = ClusterSettings.builder().hosts(asList(new ServerAddress(host, port))).build();
        final MongoClientSettings settings = MongoClientSettings.builder().clusterSettings(clusterSettings).build();
        mongoClient = MongoClients.create(settings);
        db = mongoClient.getDatabase(this.database);
        launchConsumers();
    }

    private void launchConsumers() {
        final Set<String> localInputs = inputPaths();
        this.service = Executors.newScheduledThreadPool(1);
        service.scheduleWithFixedDelay(new MongoChanFetcher(localInputs, mongoClient, database, collection, channelContext.getLocalPorts()), 0, 1, TimeUnit.SECONDS);

    }

    @Update
    public void update() {
        this.stop();
        this.start();
    }

    @Override
    public void dispatch(final String payload, final Callback callback) {
        final Document payloadDocument = new Document();
        final Object pl = getPayload(payload);
        payloadDocument.put("payload", pl);

        final Stream<? extends Document> localInputPortStream = inputPaths().stream().map(x -> decoratePort(x, payloadDocument));
        final Stream<Document> remoteInputPortStream = channelContext.getRemotePortPaths().stream().map(x -> decoratePort(x, payloadDocument));
        final Stream<Document> inputPortStream = Stream.concat(localInputPortStream, remoteInputPortStream);
        final List<Document> fullList = inputPortStream.collect(Collectors.toList());
        db.getCollection(collection).insertMany(fullList, (aVoid, throwable) -> {
            if (throwable != null) {
                Log.error(throwable.getMessage());
            }
        });

    }

    private Set<String> inputPaths() {
        ContainerRoot model = modelService.getPendingModel();
        if (model == null) {
            model = modelService.getCurrentModel().getModel();
        }
        Channel thisChan = (Channel) model.findByPath(context.getPath());
        return Util.getInputPath(thisChan, context.getNodeName());
    }

    private Document decoratePort(String path, Document doc) {
        final Document d = cloning(doc);
        d.put("port", new BsonString(path));
        return d;
    }

    private Document cloning(Document doc) {
        return Document.parse(doc.toJson());
    }

    private Object getPayload(final String payload) {
        Object pl;
        try {
            pl = Document.parse(payload);
        } catch (org.bson.json.JsonParseException e) {
            pl = payload;
        } catch (org.bson.BsonInvalidOperationException e) {
            pl = payload;
        }
        return pl;
    }

    @Stop
    public void stop() {
        mongoClient.close();
    }
}
