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
//@NodeType(description = "Represents basic node information (Infrastructure or Platform node)")
public interface PlatformNode {

    /**
     * Architecture of the node
     */

    @Param(optional = true)
    String ARCH = null;

    /**
     * Amount of memory (GB, MB, KB is allowed)
     */
    @Param(optional = true)
    String RAM = null;

    /**
     * number of cores
     */

    @Param(optional = true)
    String CPU_CORES = null;

    /**
     * Maximum amount of CPU resources
     */
    @Param(optional = true)
    String CPU_SHARES = null;
    /**
     * the disk size for the node (GB, MB, KB is allowed)
     */
    @Param(optional = true)
    String DISK_SIZE = null;
}
