package org.kevoree.library.cloud.api.helper;

import org.kevoree.ContainerNode;
import org.kevoree.DictionaryValue;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 20/01/14
 * Time: 13:07
 *
 * @author Erwan Daubert
 * @version 1.0
 */
public abstract class ResourceConstraintManager {

    public boolean defineConstraints(ContainerNode node) {
        boolean isDone = true;
        if (node.getDictionary() != null) {
            DictionaryValue dictionaryValue = node.getDictionary().findValuesByID("RAM");
            if (dictionaryValue != null) {
                isDone = isDone && defineRAM(node.getName(), dictionaryValue.getValue());
            }
            dictionaryValue = node.getDictionary().findValuesByID("CPU_CORES");
            if (dictionaryValue != null) {
                isDone = isDone && defineCPUSet(node.getName(), dictionaryValue.getValue());
            }
            dictionaryValue = node.getDictionary().findValuesByID("CPU_SHARES");
            if (dictionaryValue != null) {
                isDone = isDone && defineCPUShares(node.getName(), dictionaryValue.getValue());
            }
        }
        return isDone;
    }

    protected Long getRAM(String ram) throws NumberFormatException {
        if (ram.toLowerCase().endsWith("gb") || ram.toLowerCase().endsWith("g")) {
            return Long.parseLong(ram.substring(0, ram.length() - 2)) * 1024 * 1024 * 1024;
        } else if (ram.toLowerCase().endsWith("mb") || ram.toLowerCase().endsWith("m")) {
            return Long.parseLong(ram.substring(0, ram.length() - 2)) * 1024 * 1024;
        } else if (ram.toLowerCase().endsWith("kb") || ram.toLowerCase().endsWith("k")) {
            return Long.parseLong(ram.substring(0, ram.length() - 2)) * 1024 * 1024;
        } else {
            return Long.parseLong(ram);
        }
    }

    protected abstract boolean defineRAM(String nodeName, String value);

    protected abstract boolean defineCPUSet(String nodeName, String value);

    protected abstract boolean defineCPUShares(String nodeName, String value);

}
