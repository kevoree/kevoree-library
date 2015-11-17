package org.kevoree.library;

import groovy.util.Eval;
import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.Output;
import org.kevoree.annotation.Param;
import org.kevoree.api.Port;

/**
 * Created by mleduc on 17/11/15.
 */
@ComponentType
public class Groovy {

    @Output
    private Port result;

    @Param(optional = false, defaultValue = "1")
    public String expression;

    @Input
    public void eval(final String param) {
        final Object res = Eval.x(param, expression);

        result.send(String.valueOf(res), null);
    }
}
