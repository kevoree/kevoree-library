package org.kevoree.library.util;

import com.spotify.docker.client.messages.PortBinding;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mleduc on 05/01/16.
 */
public class PortsService {

    private final ParamService paramService = new ParamService();

    @NotNull
    public Map<String, List<PortBinding>> computePorts(final String ports) {
        final List<String> portsList = paramService.computeParamToList(ports);

        final Map<String, List<PortBinding>> portBindings = new HashMap<>();
        for (String port : portsList) {
            if (port.contains(":")) {
                final String[] split = port.split(":");
                if (split.length == 3) {
                    final String hostIp = split[0];
                    final String hostPort = split[1];
                    final String containerPort = split[2];
                    if (StringUtils.isEmpty(hostPort)) {
                        addRandomPort(portBindings, hostIp, containerPort);
                    } else {
                        addDefinedPort(portBindings, hostIp, hostPort, containerPort);
                    }
                } else {
                    final String hostPort = split[0];
                    final String containerPort = split[1];
                    if (StringUtils.isEmpty(hostPort)) {
                        addRandomPort(portBindings, "0.0.0.0", containerPort);
                    } else {
                        addDefinedPort(portBindings, "0.0.0.0", hostPort, containerPort);
                    }
                }
            } else {
                addRandomPort(portBindings, "0.0.0.0", port);
            }

        }
        return portBindings;
    }

    private void addDefinedPort(Map<String, List<PortBinding>> portBindings, String ip, String hostPort, String containerPort) {
        addPair(portBindings, containerPort, PortBinding.of(ip, hostPort));
    }

    private void addRandomPort(Map<String, List<PortBinding>> portBindings, String ip, String port) {
        addPair(portBindings, port, PortBinding.randomPort(ip));
    }

    private <T, U> void addPair(Map<T, List<U>> collec, T key, U value) {
        if (!collec.containsKey(key)) {
            collec.put(key, new ArrayList<U>());
        }
        collec.get(key).add(value);
    }
}
