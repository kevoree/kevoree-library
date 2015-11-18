package org.kevoree.library;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.Output;
import org.kevoree.annotation.Param;
import org.kevoree.api.Port;
import org.kevoree.log.Log;

import javax.script.ScriptException;

/**
 * Created by mleduc on 18/11/15.
 */
@ComponentType
public class AvatarJS {

    @Output
    private Port result;

    @Param(optional = false, defaultValue = "function retOne(a) { return 1; }")
    public String expression;

    @Param(optional = false, defaultValue = "retOne")
    public String functionName;

    public final JsEngine jsEngine = new JsEngine();

    @Input
    public void eval(final String arg) {
        try {
            final Object res = jsEngine.evaluateFunction(expression, functionName, arg);
            result.send(String.valueOf(res), null);
        } catch (ScriptException e) {
            Log.error(e.getMessage());
        } catch (NoSuchMethodException e) {
            Log.error(e.getMessage());
        }
    }
}
