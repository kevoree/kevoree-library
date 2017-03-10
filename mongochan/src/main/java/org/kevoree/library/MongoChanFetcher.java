package org.kevoree.library;


import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.kevoree.api.Port;

import java.util.Set;

import static com.mongodb.client.model.Filters.*;

/**
 *
 * Created by mleduc on 01/12/15.
 */
public class MongoChanFetcher implements Runnable {
    public static final String RECEIVED_FIELD = "received";

    private final Set<Port> localInputs;
    private final MongoClient mongoClient;
    private final String collection;
    private final String database;

    public MongoChanFetcher(Set<Port> localInputs, MongoClient mongoClient, String database, String collection) {
        this.localInputs = localInputs;
        this.mongoClient = mongoClient;
        this.collection = collection;
        this.database = database;
    }

    @Override
    public void run() {
        loop();
    }

    private void loop() {
        final MongoDatabase database = mongoClient.getDatabase(this.database);
        final MongoCollection<Document> collection = database.getCollection(this.collection);
        final Bson filter = and(or(not(exists(RECEIVED_FIELD)), eq(RECEIVED_FIELD, false)), in("port", localInputs));
        final Document update = new Document("$set", new Document(RECEIVED_FIELD, true));
        final SingleResultCallback<Document> callback = (message, throwable) -> {
            if (message != null) {
                // TODO : keep it dry.
                final String portName = message.getString("port");
                final String payload = message.getString("payload");
                for (Port p : localInputs) {
                    if (p.getPath().equals(portName)) {
                        p.send(payload);
                        break;
                    }
                }
                // once treated the message is removed from the mongodb.
                collection.deleteOne(eq("_id", message.getObjectId("_id")), null);
                loop();
            }
        };
        collection.findOneAndUpdate(filter, update, callback);
    }
}
