package org.kevoree.library.command;

import org.kevoree.DeployUnit;
import org.kevoree.KevoreeCoreException;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.AdaptationType;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.service.RuntimeService;
import org.kevoree.modeling.api.KMFContainer;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 26/01/12
 * Time: 16:35
 */

public class AddDeployUnit implements AdaptationCommand {

    private DeployUnit du;
    private RuntimeService runtimeService;

    public AddDeployUnit(DeployUnit du, RuntimeService runtimeService) {
        this.du = du;
        this.runtimeService = runtimeService;
    }

    @Override
    public void execute() throws KevoreeAdaptationException {
        ClassLoader classLoader;
        try {
            classLoader = runtimeService.installDeployUnit(du);
            if (classLoader == null) {
                throw new KevoreeAdaptationException("Unable to add DeployUnit " + du.path());
            }
        } catch (KevoreeCoreException e) {
            throw new KevoreeAdaptationException("Unable to add DeployUnit " + du.path(), e);
        }
    }

    @Override
    public void undo() throws KevoreeAdaptationException {
        new RemoveDeployUnit(du, runtimeService).execute();
    }

    @Override
    public AdaptationType getType() {
        return AdaptationType.ADD_DEPLOYUNIT;
    }

    @Override
    public int hashCode() {
        return getType().hashCode() + du.path().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AdaptationCommand && obj.hashCode() == hashCode();
    }

    @Override
    public String toString() {
        return "AddDeployUnit    " + du.path();
    }

    @Override
    public KMFContainer getElement() {
        return du;
    }
}