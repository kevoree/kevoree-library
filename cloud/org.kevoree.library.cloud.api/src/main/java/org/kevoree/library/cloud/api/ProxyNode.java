package org.kevoree.library.cloud.api;

import org.kevoree.annotation.Param;
import org.kevoree.library.java.JavaNode;

/**
 * User: Erwan Daubert - erwan.daubert@gmail.com
 * Date: 25/06/13
 * Time: 10:46
 *
 * @author Erwan Daubert
 * @version 1.0
 */
//@NodeType
public abstract class ProxyNode extends JavaNode {

    @Param(optional = true)
    protected String login;
    @Param(optional = true)
    protected String credentials;
    @Param(optional = true)
    protected String endpoint;

}
