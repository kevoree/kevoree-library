package org.kevoree.library.command;

import org.kevoree.DeployUnit;
import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.adaptation.AdaptationType;
import org.kevoree.adaptation.KevoreeAdaptationException;
import org.kevoree.service.RuntimeService;
import org.kevoree.log.Log;
import org.kevoree.modeling.api.KMFContainer;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 26/01/12
 * Time: 16:35
 */

public class RemoveDeployUnit implements AdaptationCommand {

    private DeployUnit du;
    private RuntimeService runtimeService;

    public RemoveDeployUnit(DeployUnit du, RuntimeService runtimeService) {
        this.du = du;
        this.runtimeService = runtimeService;
    }

    @Override
    public void execute() throws KevoreeAdaptationException {
        try {
            runtimeService.removeDeployUnit(du);
            Log.debug("DeployUnit {} removed", du.path());
            // TODO cleanup links
        } catch (Exception e) {
            throw new KevoreeAdaptationException("Unable to RemoveDeployUnit " + du.path(), e);
        }
    }

    @Override
    public void undo() throws KevoreeAdaptationException {
        new AddDeployUnit(du, runtimeService).execute();
    }

    @Override
    public AdaptationType getType() {
        return AdaptationType.REMOVE_DEPLOYUNIT;
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
        return "RemoveDeployUnit " + du.path();
    }

    @Override
    public KMFContainer getElement() {
        return du;
    }
}