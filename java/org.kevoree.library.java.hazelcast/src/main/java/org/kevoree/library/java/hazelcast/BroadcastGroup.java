package org.kevoree.library.java.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.*;
import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.ModelListener;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.cloner.DefaultModelCloner;
import org.kevoree.compare.DefaultModelCompare;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.ModelCloner;
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
        Config config = new Config();
        config.setClassLoader(DistributedBroadcast.class.getClassLoader());
        localHazelCast = Hazelcast.newHazelcastInstance(config);
        topic = localHazelCast.getTopic(context.getInstanceName());
        topic.addMessageListener(this);
        modelService.registerModelListener(this);
    }

    @Stop
    public void stop() {
        modelService.unregisterModelListener(this);
        localHazelCast.shutdown();
    }

    private ModelCloner cloner = new DefaultModelCloner();

    @Override
    public void onMessage(Message message) {
        if (!message.getPublishingMember().localMember()) {
            try {
                TraceSequence newtraceSeq = new DefaultTraceSequence();
                newtraceSeq.populateFromString(message.getMessageObject().toString());
                ContainerRoot clonedModel = cloner.clone(modelService.getCurrentModel().getModel());
                newtraceSeq.applyOn(clonedModel);
                modelService.update(clonedModel, new UpdateCallback() {
                    @Override
                    public void run(Boolean applied) {
                        Log.info("Model update result : " + applied);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean preUpdate(ContainerRoot currentModel, ContainerRoot proposedModel) {
        return true;
    }

    @Override
    public boolean initUpdate(ContainerRoot currentModel, ContainerRoot proposedModel) {
        return true;
    }


    @Override
    public boolean afterLocalUpdate(ContainerRoot currentModel, ContainerRoot proposedModel) {
        TraceSequence seq = compare.merge(currentModel, proposedModel);
        if (!seq.getTraces().isEmpty()) {
            topic.publish(seq.exportToString());
        }
        return true;
    }


    @Override
    public void modelUpdated() {
    }

    @Override
    public void preRollback(ContainerRoot currentModel, ContainerRoot proposedModel) {

    }

    @Override
    public void postRollback(ContainerRoot currentModel, ContainerRoot proposedModel) {

    }
}
