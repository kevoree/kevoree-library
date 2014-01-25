package org.kevoree.library.java.editor.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.kevoree.ContainerRoot;
import org.kevoree.compare.DefaultModelCompare;
import org.kevoree.impl.DefaultKevoreeFactory;
import org.kevoree.library.java.editor.model.Library;
import org.kevoree.library.java.editor.parser.HTTPMergeRequestParser;
import org.kevoree.library.java.editor.service.merge.JavaMergeService;
import org.kevoree.library.java.editor.service.merge.NpmMergeService;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.json.JSONModelSerializer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 17/01/14
 * Time: 11:29
 */
public class MergeHandler extends AbstractHandler {

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        if (s.equals("/merge")) {
            final String callback = request.getParameter("callback");
            HTTPMergeRequestParser requestParser = new HTTPMergeRequestParser();
            final Map<String, Collection<Library>> libz = requestParser.parse(request.getParameterMap().entrySet());

            DefaultKevoreeFactory factory = new DefaultKevoreeFactory();
            ContainerRoot model = factory.createContainerRoot();
            DefaultModelCompare compare = new DefaultModelCompare();

            JavaMergeService javaMerge = new JavaMergeService();
            NpmMergeService npmMerge = new NpmMergeService();
            for (Map.Entry<String, Collection<Library>> entry : libz.entrySet()) {
                if (entry.getKey().equals("java")) {
                    compare.merge(model, javaMerge.process(entry.getValue())).applyOn(model);

                } else if (entry.getKey().equals("javascript")) {
                    compare.merge(model, npmMerge.process(entry.getValue())).applyOn(model);
                }
            }

            try {
                // serialize ContainerRoot model to json
                JSONModelSerializer serializer = new JSONModelSerializer();
                String strModel = serializer.serialize(model);
                JsonParser parser = new JsonParser();
                JsonObject jsonModel = (JsonObject) parser.parse(strModel);

                // create Web Editor proper JSON response to /merge
                JsonObject jsonRes = new JsonObject();
                jsonRes.add("result", new JsonPrimitive(1));
                jsonRes.add("message", new JsonPrimitive("Ok"));
                jsonRes.add("model", jsonModel);

                // send response back as a JSONP callback
                String jsonpRes = callback + " && " + callback + "(" + jsonRes.toString() + ");";
                httpServletResponse.setDateHeader("Content-Length", jsonpRes.length());
                httpServletResponse.setContentType("text/javascript");
                httpServletResponse.getWriter().write(jsonpRes);
                request.setHandled(true);

            } catch (Exception e) {
                Log.error("ERROR /merge: " + e.getMessage());
            }
        }
    }
}