package org.kevoree.library.java.toys;

import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.Port;
import org.kevoree.library.java.core.control.OnOffSwitchFrame;
import org.kevoree.library.java.core.status.OnOffStatusDisplayFrame;
import org.kevoree.log.Log;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Displays a disabled button that gives a ON/OFF status display.
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 02/12/2013
 * Time: 12:00
 */
@ComponentType
public class ToyOnOffSwitch implements ActionListener {

    @KevoreeInject
    protected Context cmpContext;

    private OnOffSwitchFrame frame;

    @Output(optional = true)
    private Port onPressed;

    @Output(optional = true)
    private Port offPressed;

    @Output(optional = true)
    private Port togglePressed;

    @Start
    public void start() throws Exception {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame = new OnOffSwitchFrame(cmpContext.getInstanceName() + "@" + cmpContext.getNodeName());
                frame.addActionListener(ToyOnOffSwitch.this);
                frame.setVisible(true);
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


    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("ON")) {
            onPressed.send(null);
        } else if(e.getActionCommand().equals("OFF")) {
            offPressed.send(null);
        } else if(e.getActionCommand().equals("TOGGLE")) {
            togglePressed.send(null);
        }
    }
}
