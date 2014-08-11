package org.kevoree.library.defaultNodeTypes

/**
 * Created by duke on 8/7/14.
 */

class KevoreeThreadGroup(n : String) : ThreadGroup(n) {

    override fun uncaughtException(t: Thread, e: Throwable) {

        println("Kevoree as catched an exception :-)")

        super<ThreadGroup>.uncaughtException(t, e)
    }


}