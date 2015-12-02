package org.kevoree.library;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.log.Log;

@ComponentType(description = "TODO")
public class MongoDB {

    @KevoreeInject
    private Context context;

    @Param
    private String host;

    @Param(defaultValue = "27017")
    private int port = 27017;

    @Param
    private String dbName;

    @Param
    private String collection;

    private MongoClient client;
    private MongoDatabase db;

    @Start
    public void start() throws Exception {
        if (this.host == null) {
            throw new Exception("MongoDB \""+context.getInstanceName()+"\" must have a \"host\" port defined");
        }

        try {
            this.client = new MongoClient(this.host, this.port);
            this.db = this.client.getDatabase("myDb");
        } catch (Exception e) {
            Log.error("MongoDB \""+context.getInstanceName()+"\" failed to start");
            e.printStackTrace();
        }
    }

    @Stop
    public void stop() {
        if (this.client != null) {
            this.client.close();
            this.db = null;
        }
    }

    @Update
    public void update() throws Exception {
        if (this.client != null) {
            if (this.host == null) {
                this.client.close();
                this.db = null;
            } else {
                if (!this.host.equals(this.client.getAddress().getHost())) {
                    this.client.close();
                    start();
                }
            }
        }
    }

    @Input
    public void in(String json) {
        try {
            this.db.getCollection(this.collection)
                    .insertOne(Document.parse(json));
        } catch (Exception e) {
            Log.warn("Unable to parse incoming message as BSON Document");
        }
    }

    public static void main(String[] args) throws Exception {
        // dirty test
        MongoDB comp = new MongoDB();
        comp.host = "127.0.0.1";
        comp.port = 27017;
        comp.dbName = "myDb";
        comp.collection = "test";

        comp.start();

        comp.in("{ \"foo\":\"swag\"} ");
    }
}
