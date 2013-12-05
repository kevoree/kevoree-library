package org.kevoree.library.web.scaler;

import org.kevoree.ComponentInstance;
import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.ModelService;
import org.kevoree.kevscript.KevScriptEngine;
import org.kevoree.log.Log;

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

                //Collect all instance of NanoServer
                ContainerRoot model = modelService.getCurrentModel().getModel();

                //collect used ports
                List<String> ports = new ArrayList<String>();
                List<String> names = new ArrayList<String>();

                for (ContainerNode node : model.getNodes()) {
                    List<Object> selected = node.selectByQuery("components[typeDefinition.name = NanoBlogServer]");
                    for (Object loop : selected) {
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

                KevScriptEngine engine = new KevScriptEngine();
                //reactions
                if (ports.size() > target) {
                    Log.info("To much instance, drop somes");

                    for (int i = 0; i < (ports.size() - target); i++) {
                        engine.execute("delete " + names.get(0), model);
                        names.remove(names.get(0));
                    }
                } else {
                    for (int i = 0; i < (target - ports.size()); i++) {
                        Random r = new Random();
                        Integer basePort = 8010;
                        while (ports.contains(basePort.toString())) {
                            basePort++;
                        }
                        String newName = "backend_" + r.nextInt();
                        engine.execute("add " + newName + " : NanoBlogServer", model);
                        engine.execute("set " + context.getNodeName() + "." + newName + " : NanoBlogServer", model);
                    }
                }
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
