package org.kevoree.library.cloud.lxc.wrapper;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.NetworkInfo;
import org.kevoree.NetworkProperty;
import org.kevoree.api.handler.UUIDModel;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.cloner.DefaultModelCloner;
import org.kevoree.impl.DefaultKevoreeFactory;
import org.kevoree.kevscript.KevScriptEngine;
import org.kevoree.library.cloud.lxc.LXCNode;
import org.kevoree.library.cloud.lxc.wrapper.utils.IPAddressValidator;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.ModelCloner;

import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 25/06/13
 * Time: 17:34
 * To change this template use File | Settings | File Templates.
 */
public class LxcSupervision implements Runnable {

    private LXCNode lxcHostNode;
    private LxcManager lxcManager;
    private IPAddressValidator ipvalidator = new IPAddressValidator();
    private boolean starting  = true;
    public LxcSupervision(LXCNode lxcHostNode, LxcManager lxcManager) {
        this.lxcHostNode = lxcHostNode;
        this.lxcManager = lxcManager;
    }

    @Override
    public void run() {
        ModelCloner cloner = new DefaultModelCloner();
        DefaultKevoreeFactory factory = new DefaultKevoreeFactory();
        ContainerRoot model = lxcHostNode.modelService.getCurrentModel().getModel();

        List<String> lxNodes = lxcManager.getContainers();
        if (lxNodes.size() > model.getNodes().size()){
            try
            {
                model = cloner.clone(lxcHostNode.modelService.getCurrentModel().getModel());
                UUIDModel uuidModel=  lxcHostNode.modelService.getCurrentModel();
                model = lxcManager.createModelFromSystem(lxcHostNode.modelService.getNodeName(), model);
                lxcHostNode.modelService.compareAndSwap(model,uuidModel.getUUID(),new UpdateCallback() {
                    @Override
                    public void run(Boolean aBoolean) {

                    }
                });
            } catch (Exception e)
            {
                Log.error("Updating model from lxc-ls",e);
            }


        } else
        {

            for(ContainerNode n : model.getNodes()){

                if(!lxNodes.contains(n.getName())){

                    // todo remove

                }

            }

        }


        for( ContainerNode containerNode : lxcHostNode.modelService.getCurrentModel().getModel().findNodesByID(lxcHostNode.modelService.getNodeName()).getHosts()){

            if(LxcManager.isRunning(containerNode.getName())){

                String ip =   LxcManager.getIP(containerNode.getName());
                if(ip != null){

                    if(ipvalidator.validate(ip))
                    {

                        Boolean found=false;
                        for(NetworkInfo n : containerNode.getNetworkInformation()){

                            for(NetworkProperty p:  n.getValues())
                            {
                                if(p.getValue().equals(ip)){
                                    found=true;
                                }
                            }

                        }
                        if(!found)
                        {
                            Log.info("The Container {} has the IP address => {}",containerNode.getName(),ip);
                            model = cloner.clone(lxcHostNode.modelService.getCurrentModel().getModel());
                            UUIDModel uuidModel=  lxcHostNode.modelService.getCurrentModel();
                            KevScriptEngine engine = new KevScriptEngine();
                            try
                            {
                                String script ="network "+containerNode.getName()+".ip.eth0 "+ip+"\n";
                                engine.execute(script,model);
                                lxcHostNode.modelService.compareAndSwap(model,uuidModel.getUUID(),new UpdateCallback() {
                                    @Override
                                    public void run(Boolean aBoolean) {

                                    }
                                });

                            } catch (Exception e) {
                                e.printStackTrace();
                            }


                        }

                    }
                    else
                    {
                        Log.error("The format of the ip is wrong or not define");
                    }
                }


            }    else {

                if(containerNode.getStarted()){
                    Log.warn("The container {} is not running", containerNode.getName());
                    lxcManager.start_container(containerNode);
                }

            }








        }
    }
}
