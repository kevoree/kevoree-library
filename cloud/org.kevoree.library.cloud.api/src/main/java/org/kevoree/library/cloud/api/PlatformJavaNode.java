package org.kevoree.library.cloud.api;

import org.kevoree.annotation.NodeType;
import org.kevoree.annotation.Param;
import org.kevoree.library.defaultNodeTypes.JavaNode;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 20/01/14
 * Time: 09:54
 *
 * @author Erwan Daubert
 * @version 1.0
 */
@NodeType
public class PlatformJavaNode extends JavaNode implements PlatformNode {


    @Param(optional = true)
    String ARCH;
    @Param(optional = true)
    String RAM;
    @Param(optional = true)
    String CPU_CORES;
    @Param(optional = true)
    String CPU_SHARES;
    @Param(optional = true)
    String DISK_SIZE;

    @Override
    public void setARCH(String arch) {
        ARCH = arch;
    }

    @Override
    public void setRAM(String ram) {
        RAM = ram;
    }

    @Override
    public void setCPU_CORES(String cpu_cores) {
        CPU_CORES = cpu_cores;
    }

    @Override
    public void setCPU_SHARES(String cpu_shares) {
        CPU_SHARES = cpu_shares;
    }

    @Override
    public void setDISK_SIZE(String disk_size) {
        DISK_SIZE = disk_size;
    }

}
