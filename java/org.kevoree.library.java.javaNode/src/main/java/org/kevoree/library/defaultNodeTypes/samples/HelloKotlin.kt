package org.kevoree.library.defaultNodeTypes.samples

import org.kevoree.annotation.*

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 28/11/2013
 * Time: 08:37
 */

public ComponentType class HelloKotlin {

    Param val s : String = "init"

    Start fun start() {
        println("SayHello")
    }
    Stop fun stop() {
        println("Ok I go home .... :(")
    }
}
