package org.kevoree.library.helloworld;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.Param;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 28/11/2013
 * Time: 10:50
 */

@ComponentType
public class HelloJava {

    @Param
    String message;

    @KevoreeInject
    org.kevoree.api.Context context;

    @Input
    public String hello(Object msg) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Hello from ");
        buffer.append(context.getInstanceName());
        buffer.append("@");
        buffer.append(context.getNodeName());
        buffer.append("\n");
        buffer.append("path=");
        buffer.append(context.getPath());
        buffer.append("\n");
        buffer.append("message=" + message);
        return buffer.toString();
    }

}
