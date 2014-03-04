package org.kevoree.library.cloud.system;

import org.kevoree.annotation.ComponentType;
import org.kevoree.library.cloud.api.helper.ProcessStreamFileLogger;
import org.kevoree.log.Log;

import java.io.File;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 03/03/14
 * Time: 17:49
 *
 * @author Erwan Daubert
 * @version 1.0
 */
@ComponentType
public class OneLineScriptRunner extends ScriptRunner {

    @Override
    boolean runScript(String script) throws Exception {
        if (!script.contains("\n")) {
            String[] scripts = script.split(";");
            File standardOutput = new File(System.getProperty("java.io.tmpdir") + File.separator + context.getInstanceName() + ".log");
            for (String s : scripts) {
                java.lang.Process p = new ProcessBuilder(s.trim().replaceAll("[ ]+", " ").split(" ")).redirectErrorStream(true).start();
                new Thread(new ProcessStreamFileLogger(p.getInputStream(), standardOutput, true)).start();
                if (p.waitFor() != 0) {
                    Log.warn("Unable to execute command: {}. Please look at the log file: {}", script, standardOutput.getAbsolutePath());
                    return false;
                }
            }
            standardOutput.delete();
            return true;
        } else {
            Log.warn("Unable to execute an OneLineScript if it contains multiple lines...");
            return false;
        }
    }
}
