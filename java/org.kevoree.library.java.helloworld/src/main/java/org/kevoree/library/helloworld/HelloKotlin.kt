package org.kevoree.library.helloworld

import org.kevoree.annotation.*
import org.kevoree.api.Port
import org.kevoree.annotation.Library
import org.kevoree.annotation.Start
import org.kevoree.annotation.Update
import org.kevoree.annotation.Stop

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 28/11/2013
 * Time: 10:55
 */

public ComponentType Library(name="Java") class HelloKotlin {

    Param var name: String = "default" ;

    Output var out: Port? = null

    Input fun hello() {
    }

    Start fun startComponent(){}

    Stop fun stopComponent(){}

    Update fun updateComponent(){}

}
