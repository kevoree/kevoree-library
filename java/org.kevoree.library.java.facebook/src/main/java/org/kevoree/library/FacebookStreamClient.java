package org.kevoree.library;

import org.kevoree.annotation.*;
import org.kevoree.api.Port;

/**
 * Created by mleduc on 16/11/15.
 */
@ComponentType
public class FacebookStreamClient {

    @Output
    private Port stream;

    @Param(optional = false)
    private String accessToken;

    @Param(optional = false)
    private String page;

    private FacebookClientThread thread;

    @Start
    public void start() {
        this.thread = new FacebookClientThread(accessToken, page, stream);
        this.thread.start();
    }

    @Update
    public void update() {
        this.stop();
        this.start();
    }

    @Stop
    private void stop() {
        this.thread.askStop();
    }
}
