package org.kevoree.library.cloud.docker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author expi
 */
public class ContainerState {

    @JsonProperty("Running")
    private boolean running;
    
    @JsonProperty("Pid")
    private int pid;
    
    @JsonProperty("ExitCode")
    private int exitCode;
    
    @JsonProperty("StartedAt")
    private String startedAt;
    
    @JsonProperty("FinishedAt")
    private String finishedAt;

    @JsonProperty("Ghost")
    private boolean ghost;

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }

    public boolean isGhost() {
        return ghost;
    }

    public void setGhost(boolean ghost) {
        this.ghost = ghost;
    }
    
    

    @Override
    public String toString() {
        return "ContainerState{" + "running=" + running + ", pid=" + pid + ", exitCode=" + exitCode + ", startedAt=" + startedAt + ", finishedAt=" + finishedAt + ", ghost=" + ghost + '}';
    }

}
