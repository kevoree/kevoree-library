package org.kevoree.library;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;

/**
 * Created by duke on 10/12/2013.
 */
@ComponentType
public class TwoValueHello {

    @Input
    public String input(String p1, String p2) {
        return p1+"_"+p2;
    }

}
