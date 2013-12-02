package org.kevoree.sky.web;

import org.kevoree.api.ModelService;
import org.webbitserver.HttpControl;
import org.webbitserver.HttpHandler;
import org.webbitserver.HttpRequest;
import org.webbitserver.HttpResponse;

import java.nio.charset.Charset;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 09/07/13
 * Time: 13:55
 */
public class MetaDataHandler implements HttpHandler {

    private ModelService modelService = null;

    public MetaDataHandler(ModelService _modelService) {
        modelService = _modelService;
    }

    @Override
    public void handleHttpRequest(HttpRequest httpRequest, HttpResponse httpResponse, HttpControl httpControl) throws Exception {
        if (httpRequest.uri().equals("/metadata/nodeName")) {
            httpResponse.charset(Charset.forName("UTF-8"));
            httpResponse.content(modelService.getNodeName());
            httpResponse.end();
            return;
        }
        httpControl.nextHandler();
    }
}
