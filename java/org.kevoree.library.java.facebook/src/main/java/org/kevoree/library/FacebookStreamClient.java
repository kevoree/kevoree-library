package org.kevoree.library;

import org.kevoree.annotation.*;
import org.kevoree.api.Port;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by mleduc on 16/11/15.
 */
@ComponentType(version = 1)
public class FacebookStreamClient {

    @Output
    public Port stream;

    @Param(optional = false)
    public String accessToken;

    @Param(optional = false)
    public String ressourceId;

    private ScheduledExecutorService service;

    @Start
    public void start() {
        this.service = Executors.newScheduledThreadPool(1);
        final FacebookClientThread fbt = new FacebookClientThread(accessToken, ressourceId, stream);
        service.scheduleWithFixedDelay(fbt, 1, 3, TimeUnit.SECONDS);
    }

    @Update
    public void update() {
        this.stop();
        this.start();
    }

    @Stop
    private void stop() {
        this.service.shutdown();
    }
}
