package org.kevoree.library;

import fr.braindead.websocket.client.WebSocketClient;
import org.kevoree.ContainerRoot;
import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.UpdateCallback;
import org.kevoree.api.handler.UpdateContext;
import org.kevoree.api.protocol.Protocol;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.KMFContainer;
import org.kevoree.pmodeling.api.json.JSONModelLoader;
import org.kevoree.pmodeling.api.json.JSONModelSerializer;
import org.xnio.*;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.kevoree.api.protocol.Protocol.*;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 29/11/2013
 * Time: 12:07
 */

@GroupType
public class RemoteWSGroup implements Runnable {

    private static final int LOOP_BREAK = 5000;
    private static final JSONModelLoader loader = new JSONModelLoader(new DefaultKevoreeFactory());
    private static final JSONModelSerializer serializer = new JSONModelSerializer();

    @KevoreeInject
    private Context context;

    @KevoreeInject
    private ModelService modelService;

    @Param(optional = false)
    private String host;

    @Param(defaultValue = "80")
    private int port = 80;

    @Param(defaultValue = "/")
    private String path = "/";

    @Param(optional = false, fragmentDependent = true, defaultValue = "true")
    private boolean answerPull = true;

    private ScheduledExecutorService scheduledThreadPool;
    private WebSocketClient client;
    private String url;

    @Start
    public void start() throws Exception {
        url = getURL();
        scheduledThreadPool = Executors.newScheduledThreadPool(1);
        scheduledThreadPool.scheduleAtFixedRate(this, 0, LOOP_BREAK, TimeUnit.MILLISECONDS);
    }

    @Stop
    public void stop() throws IOException, InterruptedException {
        if (scheduledThreadPool != null) {
            scheduledThreadPool.shutdownNow();
        }
        if (client != null && client.isOpen()) {
            client.close();
            client = null;
        }
    }

    @Update
    public void update() throws Exception {
        url = getURL();
    }

	private String getURL() throws Exception {
		if (host == null || host.trim().length() == 0) {
			throw new Exception("'host' attribute is not specified");
		}

		if (path.startsWith("/")) {
			path = path.substring(1, path.length());
		}

		return "ws://" + host + ":" + port + "/" + path;
	}

    public void run() {
        try {
            if (context != null) {
                if (client == null || !client.isOpen()) {
                    client = new WebSocketClient(URI.create(url)) {
                        @Override
                        public void onOpen() {
                            Log.info("\"{}\" connected to {}", context.getInstanceName(), url);
                        }

                        @Override
                        public void onMessage(String msg) {
                            try {
                                Message message = Protocol.parse(msg);
                                switch (message.getType()) {
                                    case Protocol.PUSH_TYPE:
                                        Log.info("\"{}\" received a push request", context.getInstanceName());
                                        PushMessage pushMessage = (PushMessage) message;
                                        String modelStr = pushMessage.getModel();
                                        if (modelStr != null && !modelStr.isEmpty()) {
                                            try {
                                                List<KMFContainer> models = loader.loadModelFromString(modelStr);
                                                if (models != null && !models.isEmpty()) {
                                                    modelService.update((ContainerRoot) models.get(0), null);
                                                } else {
                                                    Log.warn("\"{}\" received model is empty, push aborted", context.getInstanceName());
                                                }
                                            } catch (Exception e) {
                                                Log.warn("\"{}\" unable to load received model, push aborted", context.getInstanceName());
                                            }
                                        } else {
                                            Log.warn("\"{}\" push message does not contain a model, push aborted", context.getInstanceName());
                                        }
                                        break;

                                    case Protocol.PULL_TYPE:
                                        if (answerPull) {
                                            Log.info("\"{}\" received a pull request", context.getInstanceName());
                                            try {
                                                this.send(serializer.serialize(modelService.getCurrentModel().getModel()));
                                            } catch (Exception e) {
                                                Log.warn("\"{}\" unable to serialize current model, pull aborted", context.getInstanceName());
                                            }
                                        } else {
                                            Log.info("\"{}\" received a pull request, but 'answerPull' mode is false", context.getInstanceName());
                                        }
                                        break;

                                    default:
                                        if (msg.length() > 10) {
                                            msg = msg.substring(0, 10)+"...";
                                        }
                                        Log.debug("\"{}\" unknown incoming message ({})", context.getInstanceName(), msg);
                                        break;
                                }
                            } catch (Exception e) {
                                if (msg.length() > 10) {
                                    msg = msg.substring(0, 10)+"...";
                                }
                                Log.warn("\"{}\" unable to process incoming message ({})", context.getInstanceName(), msg);
                            }
                        }

                        @Override
                        public void onClose(int code, String reason) {
                            Log.info("\"{}\" connection lost with {} (retry every {}ms)", context.getInstanceName(), url, LOOP_BREAK);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.info("\"{}\" connection problem with {} (retry every {}ms)", context.getInstanceName(), url, LOOP_BREAK);
                        }
                    };
                }
            }
        } catch (Exception e) {
            Log.error("Error while connecting to master server (is server reachable ?)");
        }
    }
}
