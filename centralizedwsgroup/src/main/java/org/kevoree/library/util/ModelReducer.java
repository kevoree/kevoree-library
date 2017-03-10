package org.kevoree.library.util;

import org.kevoree.*;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.modeling.api.KMFContainer;
import org.kevoree.modeling.api.ModelCloner;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * Created by leiko on 1/11/17.
 */
public class ModelReducer {

    private static final Pattern PKG = Pattern.compile("^/packages\\[([\\w]+)\\].*");

    public static ContainerRoot reduce(ContainerRoot model, String master, String client) {
        KevoreeFactory factory = new DefaultKevoreeFactory();
        ModelCloner cloner = factory.createModelCloner();
        ContainerRoot clonedModel = cloner.clone(model);

        reduceNodes(clonedModel, master, client);

        reduceBindings(clonedModel, master, client);

        reduceChannels(clonedModel, master, client);

        reduceGroups(clonedModel, master, client);

        reducePackages(clonedModel, master, client);

        return clonedModel;
    }

    private static void reduceNodes(ContainerRoot model, String master, String client) {
        model.getNodes().forEach(node -> {
            if (!node.getName().equals(master) && !node.getName().equals(client)) {
                boolean isRelated = false;
                for (ComponentInstance comp : node.getComponents()) {
                    if (isComponentRelated(comp, master, client)) {
                        isRelated = true;
                        break;
                    }
                }

                if (!isRelated) {
                    node.delete();
                }
            }
        });
    }

    private static void reduceBindings(ContainerRoot model, String master, String client) {
        // only check channel for bindings because the port will be checked by components
        model.getmBindings().stream()
                .filter(binding -> !isChannelRelated(binding.getHub(), master, client))
                .forEach(KMFContainer::delete);
    }

    private static void reduceChannels(ContainerRoot model, String master, String client) {
        for (Channel chan: model.getHubs()) {
            if (chan.getBindings().isEmpty()) {
                chan.delete();
            } else {
                if (!isChannelRelated(chan, master, client)) {
                    chan.delete();
                }
            }
        }
    }

    private static void reduceGroups(ContainerRoot model, String master, String client) {
        for (Group group : model.getGroups()) {
            ContainerNode node = group.findSubNodesByID(master);
            if (node == null) {
                node = group.findSubNodesByID(client);
                if (node == null) {
                    group.delete();
                }
            }
        }
    }

    private static void reducePackages(ContainerRoot model, String master, String client) {
        Set<String> usedPackages = new HashSet<>();
        ContainerNode masterNode = model.findNodesByID(master);
        ContainerNode clientNode = model.findNodesByID(client);

        if (masterNode != null) {
            usedPackages.addAll(getRelatedRootPackages(masterNode));
        }
        if (clientNode != null) {
            usedPackages.addAll(getRelatedRootPackages(clientNode));
        }

        model.getPackages().stream()
                .filter(pkg -> !usedPackages.contains(pkg.getName()))
                .forEach(KMFContainer::delete);
    }

    private static boolean isChannelRelated(Channel chan, String master, String client) {
        if (chan != null) {
            for (MBinding binding : chan.getBindings()) {
                if (binding.getPort() != null && binding.getPort().eContainer() != null) {
                    ContainerNode node = (ContainerNode) binding.getPort().eContainer().eContainer();
                    if (node.getName().equals(master) || node.getName().equals(client)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isBindingRelated(MBinding binding, String master, String client) {
        if (binding != null) {
            if (isChannelRelated(binding.getHub(), master, client)) {
                return true;
            }
            if (binding.getPort() != null && binding.getPort().eContainer() != null) {
                ContainerNode node = (ContainerNode) binding.getPort().eContainer().eContainer();
                if (node.getName().equals(master) || node.getName().equals(client)) {
                    return true;
                }

                for (MBinding b : binding.getPort().getBindings()) {
                    if (!b.path().equals(binding.path())) {
                        boolean isRelated = isBindingRelated(b, master, client);
                        if (isRelated) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean isPortRelated(Port port, String master, String client) {
        for (MBinding binding : port.getBindings()) {
            boolean isRelated = isBindingRelated(binding, master, client);
            if (isRelated) {
                return true;
            }
        }
        return false;
    }

    private static boolean isComponentRelated(ComponentInstance comp, String master, String client) {
        for (Port input : comp.getProvided()) {
            boolean isRelated = isPortRelated(input, master, client);
            if (isRelated) {
                return true;
            }
        }

        for (Port output : comp.getRequired()) {
            boolean isRelated = isPortRelated(output, master, client);
            if (isRelated) {
                return true;
            }
        }

        return false;
    }

    private static String getRootPkgName(Instance instance) {
        Matcher matcher = PKG.matcher(instance.getTypeDefinition().path());
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    private static Set<String> getRelatedRootPackages(ContainerNode node) {
        Set<String> packages = new HashSet<>();

        packages.add(getRootPkgName(node));
        packages.addAll(node.getComponents().stream().map(ModelReducer::getRootPkgName).collect(Collectors.toList()));
        packages.addAll(node.getHosts().stream().map(ModelReducer::getRootPkgName).collect(Collectors.toList()));
        packages.addAll(node.getGroups().stream().map(ModelReducer::getRootPkgName).collect(Collectors.toList()));

        return packages;
    }
}
