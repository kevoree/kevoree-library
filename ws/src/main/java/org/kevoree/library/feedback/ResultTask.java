package org.kevoree.library.feedback;

import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

public class ResultTask {

    private final ConcurrentHashMap<String, Boolean> nodesMap = new ConcurrentHashMap<>();
    private Timer timer;
    private int expectedNumberOfNodes;

    public ConcurrentHashMap<String, Boolean> getNodesMap() {
        return this.nodesMap;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public Timer getTimer() {
        return this.timer;
    }

    public int getExpectedNumberOfNodes() {

        return expectedNumberOfNodes;
    }

    public void setExpectedNumberOfNodes(int expectedNumberOfNodes) {
        this.expectedNumberOfNodes = expectedNumberOfNodes;
    }
}