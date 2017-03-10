package org.kevoree.library;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.Output;
import org.kevoree.api.Port;

/**
 * Created by mleduc on 19/05/16.
 */
@ComponentType(version = 1, description = "receives two inputs and send them to the same output")
public class Merge2 {

	@Output
	public Port output;

	@Input
	public void input0(String i) {
		output.send(i);
	}

	@Input
	public void input1(String i) {
		output.send(i);
	}
}
