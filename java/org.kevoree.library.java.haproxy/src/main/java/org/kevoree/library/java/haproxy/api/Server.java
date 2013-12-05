package org.kevoree.library.java.haproxy.api;

/**
 * Created by duke on 05/12/2013.
 */
public class Server {

    private String name;

    private String ip;

    private String port;

    private Integer maxconn;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public Integer getMaxconn() {
        return maxconn;
    }

    public void setMaxconn(Integer maxconn) {
        this.maxconn = maxconn;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("server ");
        builder.append(name);
        builder.append(" ");
        builder.append(ip);
        builder.append(":");
        builder.append(port);
        if (maxconn != null) {
            builder.append(" maxconn ");
            builder.append(maxconn);
        }
        return builder.toString();
    }

}
