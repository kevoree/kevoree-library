package org.kevoree.library.cloud.lightlxc.wrapper;

import jet.runtime.typeinfo.JetValueParameter;
import org.jetbrains.annotations.NotNull;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.ModelListenerAdapter;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.modeling.api.events.ModelElementListener;
import org.kevoree.modeling.api.events.ModelEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by root on 03/03/14.
 */
public class IpModelUpdater extends ModelListenerAdapter {

    Map<String,String> ipName = new HashMap<String, String>();

    ModelService modelservice;

    public IpModelUpdater(ModelService service){
        modelservice = service;

    }

    @Override
    public synchronized void modelUpdated() {
        StringBuffer buf = new StringBuffer();
        for (String ip  :ipName.keySet()){
            buf.append("network "+ipName.get(ip)+".ip.lan " + ip +"\n");


        }
        if (buf.length() > 0){
        modelservice.unregisterModelListener(this);
        modelservice.submitScript(buf.toString(), new UpdateCallback() {
            @Override
            public void run(Boolean aBoolean) {
                if (aBoolean) {
                    ipName.clear();

                }
                modelservice.registerModelListener(IpModelUpdater.this);

            }
        });
        }


    }

    public synchronized boolean addIpName(String ip,String name){
        if (ip == null || ipName.containsKey(ip))
            return false;
        ipName.put(ip,name);
        return true;


    }
}
