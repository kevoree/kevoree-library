package org.kevoree.library;

import org.python.core.PyException;
import org.python.util.PythonInterpreter;

/**
 * Created by mleduc on 18/11/15.
 */
public class JythonEngine {
    public String eval(final String param, final String inVariable, final String expression, final String resVariable) throws PyException {
        final PythonInterpreter pi = new PythonInterpreter();
        pi.set(inVariable, param);
        pi.exec(expression);
        return String.valueOf(pi.get(resVariable));
    }
}

