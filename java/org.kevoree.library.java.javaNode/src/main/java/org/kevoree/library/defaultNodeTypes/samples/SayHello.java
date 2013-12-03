package org.kevoree.library.defaultNodeTypes.samples;

import org.kevoree.annotation.*;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 03/12/2013
 * Time: 11:05
 */

@ComponentType
public class SayHello {

    @Param
    String message;

    @Start
    public void start() {
        System.out.println("Start called , message=" + message);
    }

    @Stop
    public void stop() {
        System.out.println("Stop called , message=" + message);
    }

    @Update
    public void update() {
        System.out.println("Update called , message=" + message);
    }

}
