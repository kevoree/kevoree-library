package org.kevoree.library;

import org.kevoree.ComponentInstance;
import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.kevscript.KevScriptEngine;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.KMFContainer;
import org.kevoree.pmodeling.api.ModelCloner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by duke on 05/12/2013.
 */
@ComponentType
public class DummyScaler implements Runnable {

    private Thread current = null;

    @Param(defaultValue = "1", optional = true)
    Integer target = null;

    @KevoreeInject
    ModelService modelService;

    @KevoreeInject
    Context context;

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
                ContainerRoot model = cloner.clone(modelService.getCurrentModel().getModel());

                //collect used ports
                List<String> ports = new ArrayList<String>();
                List<String> names = new ArrayList<String>();

                for (ContainerNode node : model.getNodes()) {
                    List<KMFContainer> selected = node.select("components[typeDefinition.name = NanoBlogServer]");
                    for (KMFContainer loop : selected) {
                        ComponentInstance instance = (ComponentInstance) loop;
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

                if (ports.size() != target) {

                    KevScriptEngine engine = new KevScriptEngine();
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
                    modelService.update(model, new UpdateCallback() {
                        @Override
                        public void run(Boolean applied) {
                            Log.info("Scaler update system");
                        }
                    });

                } else {
                    Log.info("Everything is fine ! nothing to do ...");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
