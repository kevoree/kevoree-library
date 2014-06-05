package org.kevoree.library.java.editor.cache.worker;

import com.google.gson.JsonObject;
import org.kevoree.library.java.editor.service.ServiceCallback;
import org.kevoree.library.java.editor.service.load.NpmLoadService;
import org.kevoree.log.Log;

/**
 * Created by leiko on 05/06/14.
 */
public class JSWorker extends AbstractWorker {

    @Override
    public void run() {
        NpmLoadService service = new NpmLoadService();
        service.process(new ServiceCallback() {
            @Override
            public void onSuccess(JsonObject jsonRes) {
                libraries = jsonRes;
                Log.info("JSWorker: javascript libraries cached");
            }

            @Override
            public void onError(Exception e) {
                Log.error(e.getMessage());
            }
        });
    }
}
