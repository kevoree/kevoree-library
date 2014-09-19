package org.kevoree.library.cloud.docker;

import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.NodeType;
import org.kevoree.annotation.Param;
import org.kevoree.api.ModelService;
import org.kevoree.library.cloud.docker.wrapper.DockerWrapperFactory;
import org.kevoree.library.JavaNode;
import org.kevoree.library.java.wrapper.WrapperFactory;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 21/05/2014
 * Time: 16:25
 */
@NodeType
public class DockerNode extends JavaNode {

    @Param()
    private String image;

    @Param(optional = false)
    private String commitRepo;

    @Param
    private String commitTag;

    @Param
    private String commitMsg;

    @Param
    private String commitAuthor;

    @Param
    private String cmd;

    @Param(defaultValue = "0", optional = false)
    private Integer cpuShares;

    @Param(defaultValue = "512", optional = false)
    private int memory;

    @Param()
    private String authUsername;

    @Param()
    private String authPassword;

    @Param()
    private String authEmail;

    @Param(defaultValue = "https://index.docker.io/v1/")
    private String pushRegistry;

    @Param(defaultValue = "false")
    private boolean pushOnDestroy;

    @KevoreeInject
    private ModelService modelService;

    public String getCommitRepo() {
        return commitRepo;
    }

    public String getCommitTag() {
        return commitTag;
    }

    public String getCommitMsg() {
        return commitMsg;
    }

    public String getCommitAuthor() {
        return commitAuthor;
    }

    public String getCmd() {
        return cmd;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public String getAuthEmail() {
        return authEmail;
    }

    public String getPushRegistry() {
        return pushRegistry;
    }

    public boolean getPushOnDestroy() {
        return pushOnDestroy;
    }

    public ModelService getModelService() {
        return modelService;
    }

    @Override
    protected WrapperFactory createWrapperFactory(String nodeName) {
        return new DockerWrapperFactory(nodeName, this);
    }
}