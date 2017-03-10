package org.kevoree.library.command;

import org.kevoree.adaptation.AdaptationCommand;
import org.kevoree.modeling.api.KMFContainer;

/**
 *
 * Created by leiko on 3/8/17.
 */
public abstract class AbstractAdaptationCommand implements AdaptationCommand {

    public abstract KMFContainer getElement();
}
