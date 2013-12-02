package org.kevoree.sky.web;

import org.webbitserver.HttpControl;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;
import org.webbitserver.handler.AbstractResourceHandler;
import org.webbitserver.handler.TemplateEngine;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

import static java.util.concurrent.Executors.newFixedThreadPool;

// Maybe http://www.uofr.net/~greg/java/get-resource-listing.html
public class EmbedHandler extends AbstractResourceHandler {


    public EmbedHandler(Executor ioThread, TemplateEngine templateEngine) {
        super(ioThread, templateEngine);
    }

    public EmbedHandler(Executor ioThread) {
        super(ioThread);
    }

    public EmbedHandler() {
        super(newFixedThreadPool(4));
    }


    @Override
    protected ResourceWorker createIOWorker(HttpRequest request,
                                            HttpResponse response,
                                            HttpControl control) {
        return new ResourceWorker(request, response, control);
    }

    protected class ResourceWorker extends IOWorker {

        private final HttpResponse response;

        private final HttpRequest request;

        protected ResourceWorker(HttpRequest request, HttpResponse response, HttpControl control) {
            super(request.uri(), request, response, control);
            this.response = response;
            this.request = request;
        }

        @Override
        protected boolean exists() throws IOException {
            String path2 = path;
            if (path2.equals("/")) {
                path2 = "index.html";
            }
            if (path2.startsWith("/")) {
                path2 = path2.substring(1);
            }
            return EmbedHandler.class.getClassLoader().getResource(path2) != null;
        }

        @Override
        protected boolean isDirectory() throws IOException {
            return false;
        }

        private byte[] read(InputStream content) throws IOException {
            try {
                return read(content.available(), content);
            } catch (NullPointerException happensWhenReadingDirectoryPathInJar) {
                return null;
            }
        }

        @Override
        protected byte[] fileBytes() throws IOException {
            String path2 = path;
            if (path2.equals("/")) {
                path2 = "index.html";
            }
            if (path2.startsWith("/")) {
                path2 = path2.substring(1);
            }
            return read(EmbedHandler.class.getClassLoader().getResourceAsStream(path2));
        }

        @Override
        protected byte[] welcomeBytes() throws IOException {
            read(EmbedHandler.class.getClassLoader().getResourceAsStream("index.html"));
            return null;
        }

        @Override
        protected byte[] directoryListingBytes() throws IOException {
            return null;
        }

    }
}