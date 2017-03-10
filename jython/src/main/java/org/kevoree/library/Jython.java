package org.kevoree.library;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.Output;
import org.kevoree.annotation.Param;
import org.kevoree.api.Port;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.jline.internal.Log;
import org.python.util.PythonInterpreter;

/**
 * Created by mleduc on 18/11/15.
 */
@ComponentType(version = 1)
public class Jython {
    @Output
    private Port result;

    @Param(optional = false, defaultValue = "i")
    public String inVariable;

    @Param(optional = false, defaultValue = "x")
    public String resVariable;

    @Param(optional = false, defaultValue = "x = int(i) + 1")
    public String expression;

    private final JythonEngine engine = new JythonEngine();

    @Input
    public void eval(final String param) {
        try {
            final String res = engine.eval(param, inVariable, expression, resVariable);
            result.send(res, null);
        } catch (PyException e) {
            Log.error(e.getMessage());
        }

    }


}
