package org.kevoree.library.java.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.*;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.api.handler.UpdateContext;
import org.kevoree.compare.DefaultModelCompare;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.compare.ModelCompare;
import org.kevoree.modeling.api.trace.TraceSequence;
import org.kevoree.trace.DefaultTraceSequence;

/**
 * Created by duke on 04/12/2013.
 */

@GroupType
@Library(name = "Java :: Groups")
public class BroadcastGroup implements MessageListener, ModelListener {

    @KevoreeInject
    Context context;

    private HazelcastInstance localHazelCast = null;
    private ITopic topic = null;
    private ModelCompare compare = new DefaultModelCompare();

    @KevoreeInject
    ModelService modelService;

    @Start
    public void start() {
        Log.info("Starting {}", context.getInstanceName());
        Config config = new Config();
        config.setProperty("hazelcast.logging.type", "none");
        config.setClassLoader(DistributedBroadcast.class.getClassLoader());
        localHazelCast = Hazelcast.newHazelcastInstance(config);
        topic = localHazelCast.getTopic(context.getInstanceName());
        topic.addMessageListener(this);
        modelService.registerModelListener(this);
        Log.info("{} started", context.getInstanceName());
    }

    @Stop
    public void stop() {
        Log.info("Stopping {}", context.getInstanceName());
        modelService.unregisterModelListener(this);
        localHazelCast.shutdown();
        Log.info("{} stopped", context.getInstanceName());
    }

//    private ModelCloner cloner = new DefaultModelCloner();

    @Override
    public void onMessage(Message message) {
        if (!message.getPublishingMember().localMember()) {
            Log.info("{} on {} receive a message", context.getInstanceName(), context.getNodeName());
            try {
                TraceSequence newtraceSeq = new DefaultTraceSequence();
                newtraceSeq.populateFromString(message.getMessageObject().toString());
                /*ContainerRoot clonedModel = cloner.clone(modelService.getCurrentModel().getModel());
                newtraceSeq.applyOn(clonedModel);*/
                modelService.submitSequence(newtraceSeq, new UpdateCallback() {
                    @Override
                    public void run(Boolean applied) {
                        if (applied) {
                            Log.info("{} Model update: {}", context.getInstanceName(), applied);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean preUpdate(UpdateContext updateContext) {
        return true;
    }

    @Override
    public boolean initUpdate(UpdateContext updateContext) {
        return true;
    }


    @Override
    public boolean afterLocalUpdate(UpdateContext updateContext) {
        TraceSequence seq = compare.merge(updateContext.getCurrentModel(), updateContext.getProposedModel());
        if (!seq.getTraces().isEmpty()) {
            Log.info("{} broadcast from {}", context.getInstanceName(), context.getNodeName());
            topic.publish(seq.exportToString());
        }
        return true;
    }


    @Override
    public void modelUpdated() {
    }

    @Override
    public void preRollback(UpdateContext updateContext) {

    }

    @Override
    public void postRollback(UpdateContext updateContext) {

    }
}
