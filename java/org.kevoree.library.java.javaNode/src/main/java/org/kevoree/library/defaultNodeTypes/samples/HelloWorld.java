package org.kevoree.library.defaultNodeTypes.samples;

import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.cloner.DefaultModelCloner;
import org.kevoree.modeling.api.ModelCloner;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 16/11/2013
 * Time: 10:34
 */
@ComponentType
public class HelloWorld {

    ModelCloner cloner = new DefaultModelCloner();

    @KevoreeInject
    Context context;

    @Param
    String message;

    @KevoreeInject
    ModelService modelService;

    @Output(optional = true)
    public org.kevoree.api.Port output;

    @Input(optional = true)
    public void input(Object inp) {
        System.out.println("Something arrived on the input port " + inp);
    }

    @Start
    public void start() {
        System.out.println("Context=" + context.getPath());
        System.out.println("I'm just beginning my life ! ");
        System.out.println("msg=" + message);
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(500);
                    output.call("hello " + message, null);
                } catch (InterruptedException e) {
                }
            }
        }.start();

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(3000);
                    ContainerRoot newModel = cloner.clone(modelService.getCurrentModel().getModel());
                    newModel.findNodesByID(modelService.getNodeName()).getComponents().get(0).setStarted(false);
                    modelService.update(newModel, new UpdateCallback() {
                        @Override
                        public void run(Boolean applied) {
                            System.out.println("Call after my life !");
                        }
                    });
                } catch (InterruptedException e) {
                }
            }
        }.start();

    }

    @Stop
    public void stop() {
        System.out.println("Bye all ! :-)");
    }


}
