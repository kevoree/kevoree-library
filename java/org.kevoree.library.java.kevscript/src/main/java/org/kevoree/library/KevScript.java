package org.kevoree.library;

import org.kevoree.ContainerRoot;
import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.Param;
import org.kevoree.api.KevScriptService;
import org.kevoree.api.ModelService;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.ModelCloner;

@ComponentType(version = 1, description = "Execute the defined kevscript when the trigger is called. You can use {name} to generate a unique identifier. For example \"add node0.{compname}: Ticker\" will add a Ticker to node0 with a random name.")
public class KevScript {

	@Param(optional = false)
	private String kevScript;

	@KevoreeInject
	private KevScriptService kevsService;

	@KevoreeInject
	private ModelService modelService;

	private final KevoreeFactory factory = new DefaultKevoreeFactory();
	private final ModelCloner cloner = factory.createModelCloner();

	@Input
	private void trigger(String input) {
		final ContainerRoot model = cloner.clone(modelService.getCurrentModel().getModel());
		try {
			final String scrpt = this.kevScript.replaceAll("\\{([^}]+)\\}", "%%$1%%");
			Log.debug("KEV_SCRIPT : " + scrpt);
			kevsService.execute(scrpt, model);
			modelService.update(model, null);
		} catch (Exception e) {
			Log.error(e.getMessage());
		}
	}

}
