package org.kevoree.library.java.haproxy.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by duke on 05/12/2013.
 */
public class Backend {

    private String name;

    private Boolean isDefault;

    private List<Server> servers = new ArrayList<Server>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("backend ");
        builder.append(name);
        builder.append("\n");

        builder.append("mode http\n");
        builder.append("balance roundrobin\n");

        for (Server s : servers) {
            builder.append(s.toString());
            builder.append("\n");
        }
        return builder.toString();
    }

}
