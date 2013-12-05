package org.kevoree.library.cloud.lxc.wrapper;

import org.kevoree.library.cloud.lxc.wrapper.utils.FileManager;
import org.kevoree.log.Log;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 04/12/13
 * Time: 11:40
 * To change this template use File | Settings | File Templates.
 */
public class LxcRessource {



    public static  void setlimitMemory(String id,int limit_in_bytes) throws InterruptedException, IOException
    {
        if(id != null && id.length() > 0){
            Process processcreate = new ProcessBuilder(LxcContants.lxccgroup, "-n", id, "memory.limit_in_bytes", ""+limit_in_bytes).redirectErrorStream(true).start();
            FileManager.display_message_process(processcreate.getInputStream());
            processcreate.waitFor();
        }  else {
            Log.error("setlimitMemory container id is not set");
        }
    }



    public static  void setCPUAffinity(String id,String cpus) throws InterruptedException, IOException
    {
        if(cpus != null && id != null && cpus.length() > 0 && id.length() > 0){
            //  lxc-cgroup -n node0 300000000           300M
            Process processcreate = new ProcessBuilder(LxcContants.lxccgroup, "-n", id, "cpuset.cpus", ""+cpus).redirectErrorStream(true).start();
            FileManager.display_message_process(processcreate.getInputStream());
            processcreate.waitFor();
        }  else {
            Log.error("setCPUAffinity container id is not set");
        }
    }

    public static void setlimitCPU(String id,int cpu_shares) throws InterruptedException, IOException
    {
        if(id != null && id.length() > 0){
            if(cpu_shares < 1024){
                // minimum
                cpu_shares = 1024;
            }
            Process processcreate = new ProcessBuilder(LxcContants.lxccgroup, "-n", id, "cpu.shares", ""+cpu_shares).redirectErrorStream(true).start();
            FileManager.display_message_process(processcreate.getInputStream());
            processcreate.waitFor();
        }  else {
            Log.error("setlimitCPU container id is not set");
        }
    }
}
