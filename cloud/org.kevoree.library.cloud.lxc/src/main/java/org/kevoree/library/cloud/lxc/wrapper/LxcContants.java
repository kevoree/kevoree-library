package org.kevoree.library.cloud.lxc.wrapper;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 04/12/13
 * Time: 11:40
 * To change this template use File | Settings | File Templates.
 */
public class LxcContants {

    public final static String lxcstart = "lxc-start";
    public final static String lxcstop = "lxc-stop";
    public final static String lxcclone = "lxc-clone";
    public final static String lxccreate = "lxc-create";
    public final static String lxcdestroy = "lxc-destroy";
    public final static String lxccgroup = "lxc-cgroup";
    public final static String lxcinfo = "lxc-info";
    public final static String lxcbackup = "lxc-backup";
    public final static String lxcexecute = "lxc-execute";
    public final static String lxcconsole = "lxc-console";
    public final static String lxcattach = "lxc-attach";

    // is it always true that clone is faster than create ?

    // setting constraints on start
    //    lxc-start -n toto -s lxc.arch=x86
    //    lxc-start -n toto -s lxc.cgroup.cpuset.cpus=0,1
    //    lxc-start -n toto -s lxc.cgroup.cpuset.cpu_exclusive=1 // to set the cpuset exclusive for this lxc
    //    lxc-start -n toto -s lxc.cgroup.cpu.shares=1234
    //    lxc-start -n toto -s lxc.cgroup.memory.limit_in_bytes=320000000

    // setting constraints during the execution but only for previous lxc.cgroup.* values
    //    lxc-cgroup -n toto memory.limit_in_bytes 300000000

    // disk limitation needs to be manage with lvm

    // to limit the number of file descriptor
    //sysctl -w fs.file-max=100000
    // must be run inside the container (maybe put it in the kevoree-template-specific ?)
}
