package org.kevoree.library.cloud.api;

import org.kevoree.annotation.*;
import org.kevoree.library.defaultNodeTypes.JavaNode;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 14/12/11
 * Time: 10:12
 *
 * @author Erwan Daubert
 * @version 1.0
 */
@NodeType(description = "Represents basic node information (Infrastructure or Platform node)")
public abstract class PlatformCloud extends JavaNode {

    /**
     * Architecture of the node
     */

    @Param(optional = true)
    protected String ARCH;

    /**
     * Amount of memory (GB, MB, KB is allowed)
     */
    @Param(optional = true)
    protected String RAM;

    /**
     * number of cores
     */

    @Param(optional = true)
    protected String CPU_CORES;

    /**
     * Maximum amount of CPU resources
     */
    @Param(optional = true)
    protected String CPU_SHARES;
    /**
     * the disk size for the node (GB, MB, KB is allowed)
     */
    @Param(optional = true)
    protected String DISK_SIZE;
}
