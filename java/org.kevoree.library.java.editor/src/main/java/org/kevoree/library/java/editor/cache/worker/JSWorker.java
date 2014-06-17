package org.kevoree.library.java.editor.cache.worker;

import org.kevoree.library.java.editor.service.load.NpmLoadService;
import org.kevoree.log.Log;

/**
 * Created by leiko on 05/06/14.
 */
public class JSWorker extends AbstractWorker {

    @Override
    public void run() {
        NpmLoadService service = new NpmLoadService();
        try {
            this.libraries = service.process();
            Log.info("JSWorker: javascript libraries cached");
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }
}
