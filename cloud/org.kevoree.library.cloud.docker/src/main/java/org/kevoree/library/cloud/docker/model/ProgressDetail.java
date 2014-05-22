package org.kevoree.library.cloud.docker.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by leiko on 22/05/14.
 */
public class ProgressDetail {

    @JsonProperty("current")
    private Integer current;

    @JsonProperty("total")
    private Integer total;
}
