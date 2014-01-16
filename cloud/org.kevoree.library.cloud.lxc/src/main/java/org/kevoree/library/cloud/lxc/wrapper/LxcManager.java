package org.kevoree.library.cloud.lxc.wrapper;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.impl.DefaultKevoreeFactory;
import org.kevoree.kevscript.KevScriptEngine;
import org.kevoree.library.cloud.api.helper.ProcessStreamFileLogger;
import org.kevoree.library.cloud.lxc.wrapper.utils.FileManager;
import org.kevoree.library.cloud.lxc.wrapper.utils.SystemHelper;
import org.kevoree.log.Log;

import java.io.*;
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

    final String clone_id = "baseclonekevoree";     // fix me

    private String initialTemplate;

    public LxcManager(String initialTemplate) {
        this.initialTemplate = initialTemplate;
    }


    public boolean create_container(ContainerNode node) {
        try {
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
            Log.error("create_container {} clone =>{}", node.getName(), clone_id, e);
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
            Log.error("start_container", e);
            return false;
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
     *
     * @param nodename
     * @param root
     * @return
     * @throws Exception
     */
    public boolean createModelFromSystem(String nodename, ContainerRoot root) throws Exception {

        KevScriptEngine engine = new KevScriptEngine();
        StringBuilder script = new StringBuilder();

        boolean updateIsDone = false;
        if (getContainers().size() > 0) {
            for (String node_child_id : getContainers()) {
                if (!node_child_id.equals(clone_id)) {
                    script.append("add " + nodename + "." + node_child_id + " : LXCNode\n");
                    updateIsDone = true;
                }
            }
        }
        if (updateIsDone) {
            engine.execute(script.toString(), root);
        }
        return updateIsDone;
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
            if (line.contains("RUNNING")) {
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            return false;
        }

    }


    private boolean lxc_stop_container(String id, boolean destroy) {
        try {
            Log.info("Stopping container " + id);
            Process lxcstopprocess = new ProcessBuilder(LxcContants.lxcstop, "-n", id).redirectErrorStream(true).start();

            FileManager.display_message_process(lxcstopprocess.getInputStream());
            lxcstopprocess.waitFor();
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

    public void createClone() throws Exception {
        if (!getContainers().contains(clone_id)) {
            Log.info("Creating Kevoree Base Container");
            File standardOutput = File.createTempFile(clone_id, ".log");
            Process lxccreateprocess = new ProcessBuilder(LxcContants.lxccreate, "-n", clone_id, "-t", initialTemplate).redirectErrorStream(true).start();
            new Thread(new ProcessStreamFileLogger(lxccreateprocess.getInputStream(), standardOutput)).start();
            lxccreateprocess.waitFor();

            // start Kevoree Base Container
            Log.debug("Starting Kevoree Base Container");
            Process lxcstartprocess = new ProcessBuilder(LxcContants.lxcstart, "-n", clone_id, "-d").redirectErrorStream(true).start();
            new Thread(new ProcessStreamFileLogger(lxcstartprocess.getInputStream(), standardOutput)).start();
            lxcstartprocess.waitFor();


            // configure the clone with some specificities
            Log.debug("Configuring Kevoree Base Container with some specificities");
            Process lxcConsole = new ProcessBuilder(LxcContants.lxcattach, "-n", clone_id, "--").redirectErrorStream(true).start();
            new Thread(new ProcessStreamFileLogger(lxcConsole.getInputStream(), standardOutput)).start();

            String kevoreeTemplate = new String(FileManager.load(LxcManager.class.getClassLoader().getResourceAsStream("kevoree-template-specific")));

            boolean throwException = false;
            try {
                OutputStream stream = lxcConsole.getOutputStream();
                stream.write(kevoreeTemplate.getBytes());
                stream.flush();

            } catch (IOException e) {
                lxcConsole.destroy();
            } finally {
                int result = lxcConsole.waitFor();
                if (result == 0) {
                    standardOutput.delete();
                } else {
                    Log.error("Unable to build a proper Kevoree Base Container. Please have a look to {} for more information", standardOutput.getAbsolutePath());
                    throwException = true;
                }
            }

            // stop Kevoree Base Container
            Log.debug("Stopping Kevoree Base Container");
            Process lxcstopprocess = new ProcessBuilder(LxcContants.lxcstop, "-n", clone_id).redirectErrorStream(true).start();
            new Thread(new ProcessStreamFileLogger(lxcstopprocess.getInputStream(), new File("/dev/null"))).start();
            lxcstopprocess.waitFor();

            if (throwException) {
                // do we need to remove the Kevoree Base Container ?
                Process lxcdestroyprocess = new ProcessBuilder(LxcContants.lxcdestroy, "-n", clone_id).redirectErrorStream(true).start();
                new Thread(new ProcessStreamFileLogger(lxcdestroyprocess.getInputStream(), new File("/dev/null"))).start();
                lxcdestroyprocess.waitFor();
                throw new Exception("Unable to define Kevoree Base Container.");
            }
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
    }

}
