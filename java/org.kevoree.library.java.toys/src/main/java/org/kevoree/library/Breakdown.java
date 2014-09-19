package org.kevoree.library;

import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.KevScriptService;
import org.kevoree.api.ModelService;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.ModelCloner;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 06/12/2013
 * Time: 10:00
 */
@ComponentType
public class Breakdown implements Runnable {

    @Param(defaultValue = "3000")
    Long period = 3000l;

    private ScheduledExecutorService service;
    private ScheduledFuture current;

    @Start
    public void start() {
        service = Executors.newSingleThreadScheduledExecutor();
        current = service.schedule(this, period, TimeUnit.MILLISECONDS);
    }

    @Stop
    public void stop() {
        service.shutdownNow();
    }

    @Input
    public void input(Object o) {
        Log.info("a friend ! i'm fine thanks you!");
        current.cancel(true);
        current = service.schedule(this, period, TimeUnit.MILLISECONDS);
    }

    @KevoreeInject
    ModelService modelService;

    @KevoreeInject
    Context context;

    @KevoreeInject
    KevScriptService kevScriptService;

    @Override
    public void run() {
        try {
            Log.info("i'm alone... kill myself...");
            ModelCloner cloner = new ModelCloner(new DefaultKevoreeFactory());
            ContainerRoot clonedModel = cloner.clone(modelService.getCurrentModel().getModel());
            kevScriptService.execute("remove " + context.getNodeName() + "." + context.getInstanceName(), clonedModel);
            modelService.update(clonedModel, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
