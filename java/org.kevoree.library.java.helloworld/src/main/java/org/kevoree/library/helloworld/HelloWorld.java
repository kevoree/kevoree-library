package org.kevoree.library.helloworld;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.Param;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 28/11/2013
 * Time: 10:50
 */

@ComponentType
public class HelloWorld {

    @Param
    String name;

    @Input
    public String hello(Object msg) {
        return "Hello from " + name + " reply to " + msg;
    }

}
