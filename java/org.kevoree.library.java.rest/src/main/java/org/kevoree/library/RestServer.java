package org.kevoree.library;

import io.undertow.Undertow;
import io.undertow.io.UndertowInputStream;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.kevoree.annotation.*;
import org.kevoree.api.ModelService;
import org.kevoree.library.java.ModelRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by duke on 8/26/14.
 */

@ComponentType(version = 1)
public class RestServer {

    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    private Integer port;

    @Param(defaultValue = "8090")
    public Integer getPort() {
        return port;
    }

    Undertow server;

    ModelRegistry registry;

    ExecutorService dispatcher;

    @KevoreeInject
    ModelService modelService;

    @Start
    public void start() {
        dispatcher = Executors.newSingleThreadExecutor();
        if (ModelRegistry.current.get() != null) {
            registry = ModelRegistry.current.get();
        }
        server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        if (exchange.getRequestMethod().equals(HttpString.tryFromString("POST"))) {
                            if (exchange.isInIoThread()) {
                                exchange.dispatch(this);
                                return;
                            }
                            String path = exchange.getRequestPath();
                            Future<String> future = dispatcher.submit(new ExternalMessageInjection(readBytesFromExchange(exchange), path, registry, modelService));
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                            exchange.getResponseSender().send(future.get());
                        } else {
                            String path = exchange.getRequestPath();
                            Future<String> future = dispatcher.submit(new ExternalMessageInjection(readBytesFromExchange(exchange), path, registry, modelService));
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                            exchange.getResponseSender().send(future.get());
                        }
                        exchange.getResponseHeaders().add(HttpString.tryFromString(ACCESS_CONTROL_ALLOW_ORIGIN), "*");
                        exchange.getResponseHeaders().add(HttpString.tryFromString(ACCESS_CONTROL_ALLOW_HEADERS), "*");
                        exchange.getResponseHeaders().add(HttpString.tryFromString(ACCESS_CONTROL_ALLOW_METHODS), "*");
                        exchange.endExchange();
                    }
                }).build();
        server.start();
    }

    @Stop
    public void stop() {
        if (server != null) {
            server.stop();
        }
        if (dispatcher != null) {
            dispatcher.shutdownNow();
        }
    }

    private static String readBytesFromExchange(HttpServerExchange exchange) throws IOException {
        InputStream inputStream = new UndertowInputStream(exchange);
        return getStringFromInputStream(inputStream);
    }

    private static String getStringFromInputStream(InputStream is) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }


}
