package org.kevoree.library.defaultNodeTypes

import org.kevoree.log.Log

/**
 * Created by duke on 8/7/14.
 */

class KevoreeThreadGroup(val n: String) : ThreadGroup(n) {

    override fun uncaughtException(t: Thread, e: Throwable) {
        Log.error("Uncatched exception into Kevoree component {}", e, n);
        super<ThreadGroup>.uncaughtException(t, e)
    }


}