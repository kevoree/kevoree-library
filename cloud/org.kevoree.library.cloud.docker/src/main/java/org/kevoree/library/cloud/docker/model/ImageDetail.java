package org.kevoree.library.cloud.docker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by leiko on 22/05/14.
 */
public class ImageDetail {

    @JsonProperty("status")
    private String status;

    @JsonProperty("progress")
    private String progress;

    @JsonProperty("progressDetail")
    private ProgressDetail progressDetail;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public ProgressDetail getProgressDetail() {
        return progressDetail;
    }

    public void setProgressDetail(ProgressDetail progressDetail) {
        this.progressDetail = progressDetail;
    }
}
