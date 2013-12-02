package org.kevoree.library.helloworld

import org.kevoree.annotation.*
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
