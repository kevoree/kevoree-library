package org.kevoree.library.java.editor.cache.worker;

import com.google.gson.JsonObject;
import org.kevoree.library.java.editor.service.ServiceCallback;
import org.kevoree.library.java.editor.service.load.JavaLoadService;
import org.kevoree.log.Log;

/**
 * Created by leiko on 05/06/14.
 */
public class MVNWorker extends AbstractWorker {

    private String platform;

    public MVNWorker(String platform) {
        this.platform = platform;
    }

    @Override
    public void run() {
        try {
            JavaLoadService service = new JavaLoadService(this.platform);
            this.libraries = service.process();
            Log.info("MVNWorker: "+this.platform+" libraries cached");
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }
}
