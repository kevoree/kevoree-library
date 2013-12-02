package org.kevoree.library.java.toys;

import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.library.java.core.status.GreenRedStatusFrame;
import org.kevoree.log.Log;

import javax.swing.*;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 02/12/2013
 * Time: 12:00
 */

@ComponentType
public class ToyStatusGreenRed {

    @Param
    protected Boolean initialState = false;

    @KevoreeInject
    protected Context cmpContext;

    private Boolean state;
    private GreenRedStatusFrame frame;


    @Start
    public void start() throws Exception {

        frame = new GreenRedStatusFrame((initialState?Color.GREEN:Color.RED));
        frame.setVisible(true);
        state = initialState;
        Log.trace(cmpContext.getInstanceName() + " Started");

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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setColor(Color.green);
            }
        });
    }

    @Input
    public void off() {
        state = false;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setColor(Color.RED);
            }
        });
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
