package org.kevoree.library.cloud.system;

import org.kevoree.annotation.ComponentType;
import org.kevoree.library.cloud.api.helper.ProcessStreamFileLogger;
import org.kevoree.log.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 03/03/14
 * Time: 18:31
 *
 * @author Erwan Daubert
 * @version 1.0
 */
@ComponentType
public class MultipleLinesScriptRunner extends ScriptRunner {
    @Override
    boolean runScript(String script) throws Exception {
        File scriptFile = new File(System.getProperty("java.io.tmpdir") + File.separator + context.getInstanceName());
        if (!scriptFile.exists()) {
            scriptFile.createNewFile();
        }
        scriptFile.setExecutable(true);
        if (writeScriptOnFile(script, scriptFile)) {
            File standardOutput = new File(System.getProperty("java.io.tmpdir") + File.separator + context.getInstanceName() + ".log");
            java.lang.Process p = new ProcessBuilder(scriptFile.getAbsolutePath()).redirectErrorStream(true).start();
            new Thread(new ProcessStreamFileLogger(p.getInputStream(), standardOutput, true)).start();
            if (p.waitFor() != 0) {
                Log.warn("Unable to execute command: {}. Please look at the log file: {}", script, standardOutput.getAbsolutePath());
                return false;
            }
//            scriptFile.delete();
//            standardOutput.delete();
            return true;
        } else {
            return false;
        }
    }

    private boolean writeScriptOnFile(String script, File scriptFile) {
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(scriptFile);
            stream.write(script.getBytes());
            stream.flush();
        } catch (FileNotFoundException e) {
            Log.error("{} doesn't exist.", e, scriptFile);
            return false;
        } catch (IOException e) {
            Log.error("Unable to write on {}", e, scriptFile);
            return false;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
        return true;
    }
}
