package org.kevoree.library;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Created by mleduc on 18/11/15.
 */
public class JsEngine {
    public Object evaluateFunction(final String expression, final String functionName, final String arg) throws ScriptException, NoSuchMethodException {
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("nashorn");

        engine.eval(expression);
        final Invocable inv = (Invocable) engine;
        return inv.invokeFunction(functionName, arg);
    }
}
