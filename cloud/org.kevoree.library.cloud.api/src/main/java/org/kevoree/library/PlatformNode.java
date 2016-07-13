package org.kevoree.library;

import org.kevoree.annotation.NodeType;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 14/12/11
 * Time: 10:12
 *
 * @author Erwan Daubert
 * @version 1.0
 */
@NodeType(version = 1, description = "Represents basic node information (Infrastructure or Platform node)")
public interface PlatformNode {

    /**
     * Architecture of the node
     */

//    @Param(optional = true)
//    String ARCH = null;
    void setARCH(String arch);

    /**
     * Amount of memory (GB, MB, KB is allowed)
     */
//    @Param(optional = true)
//    String RAM = null;
    void setRAM(String ram);

    /**
     * number of cores
     */

//    @Param(optional = true)
//    String CPU_CORES = null;
    void setCPU_CORES(String cpu_cores);

    /**
     * Maximum amount of CPU resources
     */
//    @Param(optional = true)
//    String CPU_SHARES = null;
    void setCPU_SHARES(String cpu_shares);
    /**
     * the disk size for the node (GB, MB, KB is allowed)
     */
//    @Param(optional = true)
//    String DISK_SIZE = null;
    void setDISK_SIZE(String disk_size);
}
