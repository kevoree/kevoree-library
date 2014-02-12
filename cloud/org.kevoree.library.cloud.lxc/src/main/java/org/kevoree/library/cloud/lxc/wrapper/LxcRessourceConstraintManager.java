package org.kevoree.library.cloud.lxc.wrapper;

import org.kevoree.library.cloud.api.helper.ProcessStreamFileLogger;
import org.kevoree.library.cloud.api.helper.ResourceConstraintManager;
import org.kevoree.log.Log;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 04/12/13
 * Time: 11:40
 * To change this template use File | Settings | File Templates.
 */
public class LxcRessourceConstraintManager extends ResourceConstraintManager {

    private final String cmd = LxcContants.lxccgroup + " -n ";

    @Override
    protected boolean defineRAM(String nodeName, String value) {
        Log.debug("Defining constraints about RAM on {}", nodeName);
        try {
            File standardOutput = File.createTempFile(nodeName, ".log");
            Process lxcConstraintsProcess = new ProcessBuilder(cmd + nodeName + " memory.limit_in_bytes=" + getRAM(value)).redirectErrorStream(true).start();
            new Thread(new ProcessStreamFileLogger(lxcConstraintsProcess.getInputStream(), standardOutput)).start();
            if (lxcConstraintsProcess.waitFor() == 0) {
                standardOutput.delete();
                return true;
            } else {
                Log.warn("Unable to define constraints about RAM on {}. Please look at {} for further information.", nodeName, standardOutput.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.warn("Unable to define constraints about RAM on {}.", e, nodeName);
        }
        return false;
    }

    @Override
    protected boolean defineCPUSet(String nodeName, String value) {
        Log.debug("Defining constraints about CPU core affinity on {}", nodeName);
        try {
            File standardOutput = File.createTempFile(nodeName, ".log");
            Process lxcConstraintsProcess = new ProcessBuilder(cmd + nodeName + " cpuset.cpus=" + value).redirectErrorStream(true).start();
            new Thread(new ProcessStreamFileLogger(lxcConstraintsProcess.getInputStream(), standardOutput)).start();
            if (lxcConstraintsProcess.waitFor() == 0) {
                standardOutput.delete();
                return true;
            } else {
                Log.warn("Unable to define constraints about CPU core affinity on {}. Please look at {} for further information.", nodeName, standardOutput.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.warn("Unable to define constraints about CPU core affinity on {}.", e, nodeName);
        }
        return false;
    }

    @Override
    protected boolean defineCPUShares(String nodeName, String value) {
        Log.debug("Defining constraints about CPU shares on {}", nodeName);
        try {
            File standardOutput = File.createTempFile(nodeName, ".log");
            Process lxcConstraintsProcess = new ProcessBuilder(cmd + nodeName + " cpu.shares=" + value).redirectErrorStream(true).start();
            new Thread(new ProcessStreamFileLogger(lxcConstraintsProcess.getInputStream(), standardOutput)).start();
            if (lxcConstraintsProcess.waitFor() == 0) {
                standardOutput.delete();
                return true;
            } else {
                Log.warn("Unable to define constraints about CPU shares on {}. Please look at {} for further information.", nodeName, standardOutput.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.warn("Unable to define constraints about CPU shares on {}.", e, nodeName);
        }
        return false;
    }
}
