package org.kevoree.library.defaultNodeTypes.samples;

import org.kevoree.annotation.GroupType;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 16/11/2013
 * Time: 11:54
 */


@GroupType
public class HelloGroup {

    @Start
    public void start() {
        System.out.println("Hello from group");
    }

    @Stop
    public void stop() {
        System.out.println("Bye from group");
    }

}
