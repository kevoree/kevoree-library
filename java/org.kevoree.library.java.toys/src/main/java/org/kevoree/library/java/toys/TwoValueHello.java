package org.kevoree.library.java.toys;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.Library;

/**
 * Created by duke on 10/12/2013.
 */
@ComponentType
@Library(name="Java :: Toys")
public class TwoValueHello {

    @Input
    public String input(String p1, String p2) {
        return p1+"_"+p2;
    }

}
