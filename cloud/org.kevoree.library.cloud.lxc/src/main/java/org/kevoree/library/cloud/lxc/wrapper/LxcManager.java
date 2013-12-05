package org.kevoree.library.cloud.lxc.wrapper;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.impl.DefaultKevoreeFactory;
import org.kevoree.kevscript.KevScriptEngine;
import org.kevoree.library.cloud.lxc.wrapper.utils.FileManager;
import org.kevoree.library.cloud.lxc.wrapper.utils.SystemHelper;
import org.kevoree.log.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 05/06/13
 * Time: 09:34
 */
public class LxcManager {

    private DefaultKevoreeFactory defaultKevoreeFactory = new DefaultKevoreeFactory();

    private String clone_id = "baseclonekevoree";     /// fix me


    private File watchdogLocalFile = null;

    public File getWatchdogLocalFile() {
        return watchdogLocalFile;
    }

    public void setWatchdogLocalFile(File watchdogLocalFile) {
        this.watchdogLocalFile = watchdogLocalFile;
    }



    public boolean create_container(ContainerNode node) {
        try
        {
            Log.debug("LxcManager : " + node.getName() + " clone =>" + clone_id);
            if (!getContainers().contains(node.getName())) {
                Log.debug("Creating container " + node.getName() + " OS " + clone_id);
                Process processcreate = new ProcessBuilder(LxcContants.lxcclone, "-o", clone_id, "-n", node.getName()).redirectErrorStream(true).start();
                FileManager.display_message_process(processcreate.getInputStream());
                processcreate.waitFor();
            } else {
                Log.warn("Container {} already exists", node.getName());
            }
        } catch (Exception e) {
            Log.error("create_container {} clone =>{}",node.getName(),clone_id, e);
            return false;
        }
        return true;
    }

    public boolean start_container(ContainerNode node) {
        try {
            Log.debug("Starting container " + node.getName());
            Process lxcstartprocess = new ProcessBuilder(LxcContants.lxcstart, "-n", node.getName(), "-d").start();
            FileManager.display_message_process(lxcstartprocess.getInputStream());
            lxcstartprocess.waitFor();
        } catch (Exception e) {
            Log.error("start_container",e);
            return  false;
        }
        return true;
    }




    public List<String> getBackupContainers() {
        List<String> containers = new ArrayList<String>();
        Process processcreate = null;
        try {
            processcreate = new ProcessBuilder("/bin/lxc-backup-list-containers").redirectErrorStream(true).start();

            BufferedReader input = new BufferedReader(new InputStreamReader(processcreate.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                containers.add(line);
            }
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return containers;
    }

    public List<String> getContainers() {
        List<String> containers = new ArrayList<String>();
        Process processcreate = null;
        try {
            processcreate = new ProcessBuilder("/bin/lxc-list-containers").redirectErrorStream(true).start();

            BufferedReader input = new BufferedReader(new InputStreamReader(processcreate.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                containers.add(line);
            }
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return containers;
    }

    /**
     * Generate the model of the system
     * @param nodename
     * @param root
     * @return
     * @throws Exception
     */
    public ContainerRoot createModelFromSystem(String nodename, ContainerRoot root) throws Exception {

        KevScriptEngine engine = new KevScriptEngine();
        StringBuilder script = new StringBuilder();

        if (getContainers().size() > 0) {
            for (String node_child_id : getContainers()) {
                if (!node_child_id.equals(clone_id)) {

                    script.append("add " + nodename + "." + node_child_id+" : JavaNode\n");
                }
            }
        }
        engine.execute(script.toString(),root);
        return root;
    }

    public synchronized static String getIP(String id) {
        String line;
        try {
            Process processcreate = new ProcessBuilder("/bin/lxc-ip", "-n", id).redirectErrorStream(true).start();
            BufferedReader input = new BufferedReader(new InputStreamReader(processcreate.getInputStream()));
            line = input.readLine();
            input.close();
            return line;
        } catch (Exception e) {
            return null;
        }

    }

    public synchronized static boolean isRunning(String id) {
        String line;
        try {
            Process processcreate = new ProcessBuilder("lxc-info", "-n", id).redirectErrorStream(true).start();
            BufferedReader input = new BufferedReader(new InputStreamReader(processcreate.getInputStream()));
            line = input.readLine();
            input.close();
            if(line.contains("RUNNING")){
                return true;
            }    else {
                return false;
            }

        } catch (Exception e) {
            return false;
        }

    }


    private boolean lxc_stop_container(String id, boolean destroy) {
        try {
            Log.info("Stoping container " + id);
            Process lxcstartprocess = new ProcessBuilder(LxcContants.lxcstop, "-n", id).redirectErrorStream(true).start();

            FileManager.display_message_process(lxcstartprocess.getInputStream());
            lxcstartprocess.waitFor();
        } catch (Exception e) {
            Log.error("lxc_stop_container ", e);
            return false;
        }
        if (destroy) {
            try {
                Log.info("Disabling the container " + id);

                Process lxcstartprocess = new ProcessBuilder(LxcContants.lxcbackup, "-n", id).redirectErrorStream(true).start();
                FileManager.display_message_process(lxcstartprocess.getInputStream());
                lxcstartprocess.waitFor();

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }


        return true;
    }

    public boolean stop_container(ContainerNode node) {
        return lxc_stop_container(node.getName(), false);
    }

    public boolean destroy_container(ContainerNode node) {
        return lxc_stop_container(node.getName(), true);
    }

    public void createClone() throws IOException, InterruptedException {
        if (!getContainers().contains(clone_id)) {
            Log.info("Creating Kevoree Base Container");
            Process lxcstartprocess = new ProcessBuilder(LxcContants.lxccreate, "-n", clone_id, "-t", "kevoree").redirectErrorStream(true).start();
            FileManager.display_message_process(lxcstartprocess.getInputStream());
            lxcstartprocess.waitFor();
        }
    }



    public void copy(String file, String path) throws IOException {
        FileManager.copyFileFromStream(LxcManager.class.getClassLoader().getResourceAsStream(file), path, file, true);
    }

    public void allow_exec(String path_file_exec) throws IOException {
        if (SystemHelper.getOS() != SystemHelper.OS.WIN32 && SystemHelper.getOS() != SystemHelper.OS.WIN64) {
            Runtime.getRuntime().exec("chmod 777 " + path_file_exec);
        } else {
            // win32
            System.err.println("ERROR");
        }
    }

    /**
     * Install scripts and template
     *
     * @throws java.io.IOException
     */
    public void install() throws IOException {
        Log.info("install lxc tools");

        copy("lxc-ip", "/bin");
        allow_exec("/bin/lxc-ip");

        copy("lxc-list-containers", "/bin");
        allow_exec("/bin/lxc-list-containers");

        copy("lxc-backup", "/bin");
        allow_exec("/bin/lxc-backup");

        copy("lxc-backup-list-containers", "/bin");
        allow_exec("/bin/lxc-backup-list-containers");

        copy("lxc-restore", "/bin");
        allow_exec("/bin/lxc-restore");

        copy("lxc-backup-list-containers", "/bin");
        allow_exec("/bin/lxc-backup-list-containers");



        String version =   defaultKevoreeFactory.getVersion();

        String kevoreeTemplate =    new String(FileManager.load(LxcManager.class.getClassLoader().getResourceAsStream("lxc-kevoree")));
        kevoreeTemplate  = kevoreeTemplate.replace("$KEVOREE-VERSION$",version);

        kevoreeTemplate = kevoreeTemplate.replace("$KEVOREE-WATCHDOG$",watchdogLocalFile.getAbsolutePath());

        FileManager.writeFile("/usr/share/lxc/templates/lxc-kevoree",kevoreeTemplate,false);

        allow_exec("/usr/share/lxc/templates/lxc-kevoree");
    }

}
