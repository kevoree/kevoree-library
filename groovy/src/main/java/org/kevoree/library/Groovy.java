package org.kevoree.library;

import groovy.util.Eval;
import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.Output;
import org.kevoree.annotation.Param;
import org.kevoree.api.Port;

/**
 *
 * Created by mleduc on 17/11/15.
 */
@ComponentType(version = 1)
public class Groovy {

    @Output
    private Port result;

    @Param(optional = false)
    public String expression = "1";

    @Input
    public void eval(final String param) {
        final Object res = Eval.x(param, expression);

        result.send(String.valueOf(res), null);
    }
}
