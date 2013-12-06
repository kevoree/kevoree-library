package org.kevoree.library.helloworld;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.Library;
import org.kevoree.api.Context;

/**
 * Created by duke on 05/12/2013.
 */
@ComponentType
@Library(name = "Java :: Hello")
public class ConsolePrinter {

    @KevoreeInject
    Context context;

    @Input
    public void input(Object msg) {
        System.out.println(context.getInstanceName()+">"+msg);
    }

}