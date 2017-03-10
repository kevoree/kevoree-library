package org.kevoree.library.util;

import org.kevoree.*;

import java.util.HashMap;

/**
 *
 * Created by leiko on 1/11/17.
 */
public class GroupHelper {

    public static ContainerNode findMasterNode(Group group) {
        for (FragmentDictionary fDic : group.getFragmentDictionary()) {
            Value isMaster = fDic.findValuesByID("isMaster");
            if (isMaster != null && isMaster.getValue().equals("true")) {
                ContainerNode subNode = group.findSubNodesByID(fDic.getName());
                if (subNode != null) {
                    return subNode;
                }
            }
        }

        return null;
    }

    public static HashMap<String, HashMap<String, String>> findMasterNets(Group group, ContainerNode masterNode) {
        HashMap<String, HashMap<String, String>> nets = new HashMap<>();
        ContainerNode node = group.findSubNodesByID(masterNode.getName());
        if (node != null) {
            for (NetworkInfo net : node.getNetworkInformation()) {
                HashMap<String, String> values = new HashMap<>();
                nets.put(net.getName(), values);
                for (Value val : net.getValues()) {
                    values.put(val.getName(), val.getValue());
                }
            }
        }

        return nets;
    }
}
