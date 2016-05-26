package org.kevoree.library;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.Output;
import org.kevoree.api.Port;

/**
 * Created by mleduc on 19/05/16.
 */
@ComponentType(description = "Take an input and send it to three outputs")
public class Fork3 {

    @Output
    public Port port0;

    @Output
    public Port port1;

    @Output
    public Port port2;

    @Input
    public void input(String input) {
        port0.send(input);
        port1.send(input);
        port2.send(input);
    }
}
