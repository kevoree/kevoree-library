package org.kevoree.library;

import com.mongodb.ServerAddress;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.connection.ClusterSettings;
import org.bson.BsonString;
import org.bson.Document;
import org.kevoree.annotation.*;
import org.kevoree.api.Callback;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.service.ModelService;
import org.kevoree.log.Log;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

//import org.bson.*;

/**
 * Created by mleduc on 25/11/15.
 */
@org.kevoree.annotation.ChannelType(version = 1)
public class MongoChan implements ChannelDispatch {

    @Param(optional = false)
    private String host = "localhost";

    @Param(optional = false)
    private int port = 27017;

    @Param(optional = false)
    private String database;

    @Param(optional = false)
    private String collection;

    private MongoDatabase db;
    private MongoClient mongoClient;

    @KevoreeInject
    private ChannelContext context;

    @KevoreeInject
    private ModelService modelService;

    private ScheduledExecutorService service;

    @Start
    public void start() {
        final ClusterSettings clusterSettings = ClusterSettings.builder().hosts(asList(new ServerAddress(host, port))).build();
        final MongoClientSettings settings = MongoClientSettings.builder().clusterSettings(clusterSettings).build();
        mongoClient = MongoClients.create(settings);
        db = mongoClient.getDatabase(this.database);
        launchConsumers();
    }

    @Stop
    public void stop() {
        mongoClient.close();
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

        final Stream<? extends Document> localInputPortStream = context.getLocalInputs()
                .stream().map(p -> decoratePort(p.getPath(), payloadDocument));
        final Stream<Document> remoteInputPortStream = context.getRemoteInputs()
                .stream().map(p -> decoratePort(p.getPath(), payloadDocument));
        final Stream<Document> inputPortStream = Stream.concat(localInputPortStream, remoteInputPortStream);
        final List<Document> fullList = inputPortStream.collect(Collectors.toList());
        db.getCollection(collection).insertMany(fullList, (aVoid, throwable) -> {
            if (throwable != null) {
                Log.error(throwable.getMessage());
            }
        });

    }

    private void launchConsumers() {
        this.service = Executors.newScheduledThreadPool(1);
        service.scheduleWithFixedDelay(new MongoChanFetcher(context.getLocalInputs(), mongoClient, database, collection), 0, 1, TimeUnit.SECONDS);

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
        } catch (org.bson.json.JsonParseException | org.bson.BsonInvalidOperationException e) {
            pl = payload;
        }
        return pl;
    }
}
