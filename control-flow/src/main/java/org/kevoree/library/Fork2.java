package org.kevoree.library;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.Output;
import org.kevoree.api.Port;

/**
 * Created by mleduc on 19/05/16.
 */
@ComponentType(version = 1, description = "Take an input and send it to two outputs")
public class Fork2 {

	@Output
	public Port port0;

	@Output
	public Port port1;

	@Input
	public void input(String input) {
		port0.send(input);
		port1.send(input);
	}
}
