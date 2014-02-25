package org.kevoree.library.cloud.lightlxc.wrapper

/**
 * Created by root on 25/02/14.
 */
object MkNodeCommandExecutor{

    public fun mkNode(baseDir: String, childName:String) {
        val command = "mknod"
        val args = array(command, baseDir + "/dev/null","c","1","3")
        execute(childName,args)
        val args1 = array(command, baseDir + "/dev/zero","c","1","5")
        execute(childName,args1)
        val args2 = array(command, baseDir + "/dev/console","c","5","0")
        execute(childName,args2)
        val args3 = array(command, baseDir + "/dev/tty","c","5","0")
        execute(childName,args3)
        val args4 = array(command, baseDir + "/dev/tty0","c","4","0")
        execute(childName,args4)
        val args5 = array(command, baseDir + "/dev/tty1","c","4","0")
        execute(childName,args5)
        val args6 = array(command, baseDir + "/dev/tty5","c","4","0")
        execute(childName,args6)
        val args7 = array(command, baseDir + "/dev/random","c","1","8")
        execute(childName,args7)
        val args8 = array(command, baseDir + "/dev/urandom","c","1","9")
        execute(childName,args8)
        val args9 = array(command, baseDir + "/dev/ram0","b","1","0")
        execute(childName,args9)
        val args10 = array("chmod", "a+rwx"  ,baseDir+"/dev/null",
                baseDir+"/dev/zero",
                baseDir+"/dev/console",
                baseDir+"/dev/tty",
                baseDir+"/dev/tty0",
                baseDir+"/dev/tty1",
                baseDir+"/dev/tty5",
                baseDir+"/dev/random",
                baseDir+"/dev/urandom",
                baseDir+"/dev/ram0")
        execute(childName,args10)
    }

    public fun execute( childName:String, args:Array<String>) {
        val process1 = Runtime.getRuntime().exec(args)
        val readerOUTthread1 = Thread(Reader(process1.getInputStream()!!, childName, false))
        val readerERRthread1 = Thread(Reader(process1.getErrorStream()!!, childName, true))
        readerOUTthread1.start()
        readerERRthread1.start()
        Runtime.getRuntime().exec(args).waitFor()
    }
}