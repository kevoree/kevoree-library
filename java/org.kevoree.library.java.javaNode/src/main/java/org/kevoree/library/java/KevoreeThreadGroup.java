package org.kevoree.library.java;

import org.kevoree.log.Log;

/**
 * Created by duke on 9/26/14.
 */
public class KevoreeThreadGroup extends ThreadGroup{

    private String n;

    public KevoreeThreadGroup(String n) {
        super(n);
        this.n = n;
    }

    public void uncaughtException(Thread t, Throwable e) {
        Log.error("Uncatched exception into Kevoree component {}", e, n);
    }
}
