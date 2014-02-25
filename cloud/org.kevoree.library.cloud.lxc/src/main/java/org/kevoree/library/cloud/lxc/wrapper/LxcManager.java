package org.kevoree.library.cloud.lxc.wrapper;

import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.DictionaryValue;
import org.kevoree.impl.DefaultKevoreeFactory;
import org.kevoree.kevscript.KevScriptEngine;
import org.kevoree.library.cloud.api.helper.ProcessStreamFileLogger;
import org.kevoree.library.cloud.api.helper.ResourceConstraintManager;
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
    private ResourceConstraintManager constraintManager;

    public LxcManager(String initialTemplate, ResourceConstraintManager constraintManager) {
        this.initialTemplate = initialTemplate;
        this.constraintManager = constraintManager;
    }

    public boolean createContainer(ContainerNode node) {
        try {
            if (!getContainers().contains(node.getName())) {
                File standardOutput = new File(System.getProperty("java.io.tmpdir") + File.separator + node.getName() + ".log");
                Log.debug("Creating container {} using {} as clone", node.getName(), clone_id);
                Process processcreate = new ProcessBuilder(LxcContants.lxcclone, "-o", clone_id, "-n", node.getName()).redirectErrorStream(true).start();
                new Thread(new ProcessStreamFileLogger(processcreate.getInputStream(), standardOutput, true)).start();
                if (processcreate.waitFor() == 0) {
                    standardOutput.delete();
                    return true;
                } else {
                    Log.error("Unable to create container {} using clone {}. Please look at {} for further information.", node.getName(), clone_id, standardOutput.getAbsolutePath());
                    return false;
                }
            } else {
                Log.debug("Container {} already exists so it is not created", node.getName());
                return true;
            }
        } catch (Exception e) {
            Log.error("Unable to create container {} using clone {}", e, node.getName(), clone_id);
            return false;
        }
    }

    public boolean startContainer(ContainerNode node) {
        String[] cmd = new String[]{LxcContants.lxcstart, "-n", node.getName(), "-d"};
        if (node.getDictionary() != null) {
            DictionaryValue dictionaryValue = node.getDictionary().findValuesByID("ARCH");
            if (dictionaryValue != null) {
                cmd = new String[]{LxcContants.lxcstart, "-n", node.getName(), "-d", "-s", "lxc.arch=" + dictionaryValue.getValue()};
            }
        }
        try {
            File standardOutput = new File(System.getProperty("java.io.tmpdir") + File.separator + node.getName() + ".log");
            Log.debug("Starting container {}", node.getName());
            Process lxcstartprocess = new ProcessBuilder(cmd).start();
            new Thread(new ProcessStreamFileLogger(lxcstartprocess.getInputStream(), standardOutput, true)).start();
            if (lxcstartprocess.waitFor() == 0) {
                standardOutput.delete();
                constraintManager.defineConstraints(node);
                return true;
            } else {
                Log.error("Unable to start the container {}. Please look at {} for further information.", node.getName(), standardOutput.getAbsolutePath());
                return false;
            }
        } catch (Exception e) {
            Log.error("Unable to start the container {}", e, node.getName());
            return false;
        }
    }

    public boolean defineConstraints(ContainerNode node) {
        return constraintManager.defineConstraints(node);
    }

    public List<String> getBackupContainers() {
        List<String> containers = new ArrayList<String>();
        Process processcreate;
        try {
            processcreate = new ProcessBuilder("/bin/lxc-backup-list-containers").redirectErrorStream(true).start();

            BufferedReader input = new BufferedReader(new InputStreamReader(processcreate.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                containers.add(line);
            }
            input.close();
        } catch (IOException e) {
            Log.debug("Unable to get backup containers list", e);
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
            Log.debug("Unable to get containers list", e);
        }
        return containers;
    }

    /**
     * Generate the model of the system
     *
     * @param nodeName
     * @param root
     * @return
     */
    public boolean createModelFromSystem(String nodeName, ContainerRoot root) {

        KevScriptEngine engine = new KevScriptEngine();
        StringBuilder script = new StringBuilder();

        boolean updateIsNeeded = false;
        if (getContainers().size() > 0) {
            for (String node_child_id : getContainers()) {
                if (!node_child_id.equals(clone_id)) {
                    script.append("add ").append(nodeName).append(".").append(node_child_id).append(" : LXCNode\n");
                    if (isRunning(nodeName)) {
                        script.append("set ").append(nodeName).append(".started = 'true'\n");
                    } else {
                        script.append("set ").append(nodeName).append(".started = 'false'\n");
                    }
                    updateIsNeeded = true;
                }
            }
        }
        if (updateIsNeeded) {
            try {
                engine.execute(script.toString(), root);
                return true;
            } catch (Exception e) {
                Log.debug("Unable to apply the kevScript:\n{}", e, script.toString());
                return false;
            }
        } else {
            return false;
        }
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
            Log.debug("Unable to find IP for container {}", e, id);
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
            return line.contains("RUNNING");

        } catch (Exception e) {
            Log.debug("Unable to know if container {} is running", e, id);
            return false;
        }

    }


    private boolean stopNDestroy(String id, boolean destroy) {
        if (!destroy) {
            Log.info("Stopping container " + id);
        } else {
            Log.info("Destroying container " + id);
        }
        try {
            boolean done;
            File standardOutput = new File(System.getProperty("java.io.tmpdir") + File.separator + id + ".log");
            Process process;
            if (isRunning(id)) {
                process = new ProcessBuilder(LxcContants.lxcstop, "-n", id).redirectErrorStream(true).start();

                new Thread(new ProcessStreamFileLogger(process.getInputStream(), standardOutput, true)).start();
                done = process.waitFor() == 0;

                if (done) {
                    standardOutput.delete();
                }
            } else {
                done = true;
            }
            if (done) {
                if (destroy) {
                    try {
                        process = new ProcessBuilder(LxcContants.lxcbackup, "-n", id).redirectErrorStream(true).start();
                        new Thread(new ProcessStreamFileLogger(process.getInputStream(), standardOutput, true)).start();
                        if (process.waitFor() == 0) {
                            standardOutput.delete();
                            return true;
                        } else {
                            Log.warn("Unable to destroy container {}. Please look at {} for further information.", id, standardOutput.getAbsolutePath());
                            return false;
                        }
                    } catch (Exception e) {
                        Log.error("Unable to destroy the container {}", e, id);
                        return false;
                    }
                } else {
                    return true;
                }
            } else {
                Log.warn("Unable to stop container {}. Please look at {} for further information.", id, standardOutput.getAbsolutePath());
                return false;
            }
        } catch (Exception e) {
            Log.error("Unable to stop the container {}", e, id);
            return false;
        }
    }

    public boolean stopContainer(ContainerNode node) {
        return stopNDestroy(node.getName(), false);
    }

    public boolean destroyContainer(ContainerNode node) {
        return stopNDestroy(node.getName(), true);
    }

    public void createClone() throws Exception {
        if (!getContainers().contains(clone_id)) {
            boolean throwException = false;
            Log.info("Creating Kevoree Base Container");
            File standardOutput = new File(System.getProperty("java.io.tmpdir") + File.separator + clone_id + ".log");
            Process lxccreateprocess = new ProcessBuilder(LxcContants.lxccreate, "-n", clone_id, "-t", initialTemplate).redirectErrorStream(true).start();
            new Thread(new ProcessStreamFileLogger(lxccreateprocess.getInputStream(), standardOutput, true)).start();
            if (lxccreateprocess.waitFor() == 0) {
                // start Kevoree Base Container
                Log.debug("Starting Kevoree Base Container");
                Process lxcstartprocess = new ProcessBuilder(LxcContants.lxcstart, "-n", clone_id, "-d").redirectErrorStream(true).start();
                new Thread(new ProcessStreamFileLogger(lxcstartprocess.getInputStream(), standardOutput, true)).start();
                if (lxcstartprocess.waitFor() == 0) {
                    // configure the clone with some specificities
                    Log.debug("Configuring Kevoree Base Container with some specificities");
                    Process lxcConsole = new ProcessBuilder(LxcContants.lxcattach, "-n", clone_id, "--").redirectErrorStream(true).start();
                    new Thread(new ProcessStreamFileLogger(lxcConsole.getInputStream(), standardOutput, true)).start();

                    String kevoreeTemplate = new String(FileManager.load(LxcManager.class.getClassLoader().getResourceAsStream("kevoree-template-specific")));

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
                    new Thread(new ProcessStreamFileLogger(lxcstopprocess.getInputStream(), new File("/dev/null"), false)).start();
                    lxcstopprocess.waitFor();

                    Log.info("Kevoree Base Container created");
                } else {
                    Log.error("Unable to start Kevoree Base Container to finish its configuration. Please have a look to {} for more information", standardOutput.getAbsolutePath());
                    throwException = true;
                }
            } else {
                Log.error("Unable to create Kevoree Base Container. Please have a look to {} for more information", standardOutput.getAbsolutePath());
                throwException = true;
            }
            if (throwException) {
                // do we need to remove the Kevoree Base Container ?
                Process lxcdestroyprocess = new ProcessBuilder(LxcContants.lxcdestroy, "-n", clone_id).redirectErrorStream(true).start();
                new Thread(new ProcessStreamFileLogger(lxcdestroyprocess.getInputStream(), new File("/dev/null"), false)).start();
                lxcdestroyprocess.waitFor();
                throw new Exception("Unable to define Kevoree Base Container.");
            }
        }
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

        FileManager.copyFileFromStream(LxcManager.class.getClassLoader().getResourceAsStream("lxc-ip"), "/bin", "lxc-ip", true);
        allow_exec("/bin/lxc-ip");

        FileManager.copyFileFromStream(LxcManager.class.getClassLoader().getResourceAsStream("lxc-list-containers"), "/bin", "lxc-list-containers", true);
        allow_exec("/bin/lxc-list-containers");

        FileManager.copyFileFromStream(LxcManager.class.getClassLoader().getResourceAsStream("lxc-backup"), "/bin", "lxc-backup", true);
        allow_exec("/bin/lxc-backup");

        FileManager.copyFileFromStream(LxcManager.class.getClassLoader().getResourceAsStream("lxc-backup-list-containers"), "/bin", "lxc-backup-list-containers", true);
        allow_exec("/bin/lxc-backup-list-containers");

        FileManager.copyFileFromStream(LxcManager.class.getClassLoader().getResourceAsStream("lxc-restore"), "/bin", "lxc-restore", true);
        allow_exec("/bin/lxc-restore");

        FileManager.copyFileFromStream(LxcManager.class.getClassLoader().getResourceAsStream("lxc-backup-list-containers"), "/bin", "lxc-backup-list-containers", true);
        allow_exec("/bin/lxc-backup-list-containers");
    }
}
