package org.kevoree.library;

import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.library.java.core.status.OnOffStatusDisplayFrame;
import org.kevoree.log.Log;

import javax.swing.*;

/**
 * Displays a disabled button that gives a ON/OFF status display.
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 02/12/2013
 * Time: 12:00
 */
@ComponentType
public class ToyOnOffStatusDisplay {

    @Param(defaultValue = "false")
    protected Boolean initialState = false;

    @KevoreeInject
    protected Context cmpContext;

    private Boolean state;
    private OnOffStatusDisplayFrame frame;


    @Start
    public void start() throws Exception {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame = new OnOffStatusDisplayFrame(cmpContext.getInstanceName() + "@" + cmpContext.getNodeName(), initialState);
                frame.setVisible(true);
                state = initialState;
                Log.trace(cmpContext.getInstanceName() + " Started");
            }});
    }

    @Stop
    public void stop() {
        frame.dispose();
        frame = null;
    }

    @Update
    public void update() {
    }

    @Input
    public void on() {
        state = true;
        frame.setStatus(true);
    }

    @Input
    public void off() {
        state = false;
        frame.setStatus(false);
    }

    @Input
    public void toggle() {
        if (state) {
            this.off();
        } else {
            this.on();
        }
    }
}
