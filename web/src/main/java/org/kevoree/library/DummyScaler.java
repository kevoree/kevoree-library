package org.kevoree.library;

import org.kevoree.ComponentInstance;
import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.service.KevScriptService;
import org.kevoree.service.ModelService;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.ModelCloner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by duke on 05/12/2013.
 */
@ComponentType(version = 1)
public class DummyScaler implements Runnable {

    private Thread current = null;

    @Param(defaultValue = "1", optional = true)
    Integer target = null;

    @KevoreeInject
    ModelService modelService;

    @KevoreeInject
    Context context;

    @KevoreeInject
    KevScriptService engine;

    @Start
    public void start() {
        current = new Thread(this);
        current.start();
    }

    @Stop
    public void stop() {
        current.stop();
    }

    private static final String propName = "http_port";

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(5000);
                //Collect all instance of NanoServer
                ModelCloner cloner = new ModelCloner(new DefaultKevoreeFactory());
                ContainerRoot model = cloner.clone(modelService.getCurrentModel());
                //collect used ports
                List<String> ports = new ArrayList<String>();
                List<String> names = new ArrayList<String>();
                for (ContainerNode node : model.getNodes()) {
                    for(ComponentInstance instance : node.getComponents()){
                        if(instance.getTypeDefinition().getName().equals("NanoBlogServer")){
                            if (instance.getDictionary() != null) {
                                if (instance.getDictionary().findValuesByID(propName) != null) {
                                    ports.add(instance.getDictionary().findValuesByID(propName).getValue());
                                    names.add(instance.getName());
                                }
                            } else {
                                if (instance.getTypeDefinition().getDictionaryType().findAttributesByID(propName).getDefaultValue() != null) {
                                    ports.add(instance.getTypeDefinition().getDictionaryType().findAttributesByID(propName).getDefaultValue());
                                    names.add(instance.getName());
                                }
                            }
                        }
                    }
                }
                if (ports.size() != target) {

                    //reactions
                    if (ports.size() > target) {
                        Log.info("To much instances, drop some");
                        while (names.size() > target) {
                            String toDrop = names.get(0);
                            names.remove(toDrop);
                            engine.execute("remove " + context.getNodeName() + "." + toDrop, model);
                            Log.info("Scaler drop " + toDrop);
                        }
                    } else {
                        while (ports.size() < target) {
                            Log.info("No enough instances, add some");
                            Random r = new Random();
                            Integer basePort = 8010;
                            while (ports.contains(basePort.toString())) {
                                basePort++;
                            }
                            String newName = "backend_" + Math.abs(r.nextInt());
                            engine.execute("add " + context.getNodeName() + "." + newName + " : NanoBlogServer", model);
                            engine.execute("set " + context.getNodeName() + "." + newName + ".http_port = \"" + basePort.toString() + "\"", model);
                            ports.add(basePort.toString());
                        }
                    }
                    Log.info("Nb instances is fine now :-)");
                    modelService.update(model, e -> Log.info("Scaler update system"));

                } else {
                    Log.info("Everything is fine ! nothing to do ...");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
