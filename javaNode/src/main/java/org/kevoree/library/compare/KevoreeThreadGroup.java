package org.kevoree.library.compare;

import org.kevoree.log.Log;

/**
 *
 * Created by duke on 9/26/14.
 */
public class KevoreeThreadGroup extends ThreadGroup {

    private String instancePath;

    public KevoreeThreadGroup(String instancePath) {
        super("kev_instance_" + instancePath);
        this.instancePath = instancePath;
    }

    public void uncaughtException(Thread t, Throwable e) {
        Log.error("Uncaught exception in Thread: {}", e, instancePath);
    }
}
