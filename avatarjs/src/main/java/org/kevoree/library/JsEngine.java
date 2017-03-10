package org.kevoree.library;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Created by mleduc on 18/11/15.
 */
public class JsEngine {
    public String evaluateFunction(final String expression, final String functionName, final String arg) throws ScriptException, NoSuchMethodException {
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("nashorn");
        engine.eval(expression);
        return String.valueOf(((Invocable) engine).invokeFunction(functionName, arg));
    }
}
