package org.kevoree.library.helloworld

import org.kevoree.annotation.ComponentType
import org.kevoree.annotation.Param
import org.kevoree.annotation.Input
import org.kevoree.annotation.Output
import org.kevoree.api.Port

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 28/11/2013
 * Time: 10:55
 */

public ComponentType class HelloKotlin {

    Param var name: String = "default" ;

    Output var out: Port? = null

    Input fun hello() {

    }

}
