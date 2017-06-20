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

    @Param(optional = false)
    public String inVariable = "i";

    @Param(optional = false)
    public String resVariable = "x";

    @Param(optional = false)
    public String expression = "x = int(i) + 1";

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
