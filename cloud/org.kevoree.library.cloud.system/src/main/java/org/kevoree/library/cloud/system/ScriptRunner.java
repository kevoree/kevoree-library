package org.kevoree.library.cloud.system;

import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.log.Log;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 03/03/14
 * Time: 17:44
 *
 * @author Erwan Daubert
 * @version 1.0
 */
@ComponentType
public abstract class ScriptRunner {
    @Param(optional = false)
    protected String startScript;
    @Param(optional = false)
    protected String stopScript;

    @KevoreeInject
    protected Context context;

    @Start
    public void start() throws Exception {
        Log.debug("Starting {} with command: {}", context.getInstanceName(), startScript);
        Log.info("{} is started. startScript executed: {}", context.getInstanceName(), runScript(startScript));
    }

    @Stop
    public void stop() throws Exception {
        Log.debug("Stopping {} with command: {}", context.getInstanceName(), stopScript);
        Log.info("{} is stopped stopScript executed: {}", context.getInstanceName(), runScript(stopScript));
    }

    abstract boolean runScript(String script) throws Exception;
}
