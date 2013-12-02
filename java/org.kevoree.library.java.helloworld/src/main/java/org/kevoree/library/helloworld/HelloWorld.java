package org.kevoree.library.helloworld;

import org.kevoree.annotation.*;

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

    @Start
    public void startComponent() {}

    @Stop
    public void stopComponent() {}

    @Update
    public void updateComponent() {}

}
