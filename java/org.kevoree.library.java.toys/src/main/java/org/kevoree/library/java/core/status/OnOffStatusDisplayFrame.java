package org.kevoree.library.java.core.status;

import org.kevoree.log.Log;

import javax.swing.*;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 02/12/2013
 * Time: 11:58
 */
public class OnOffStatusDisplayFrame extends JFrame {

    private JLabel statusLabel;
    private JToggleButton statusButton;
    private boolean state;

    public OnOffStatusDisplayFrame(String name, boolean initialState) {
        super(name);
        this.state = initialState;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        initGraphicalComponents();
        layoutGraphicalComponents();

        pack();
    }


    private void initGraphicalComponents() {
        statusLabel = new JLabel("Status:");
        statusButton = new JToggleButton((state?"ON":"OFF"));
        statusButton.setSelected(state);
        statusButton.setEnabled(false);
    }

    private void layoutGraphicalComponents() {
        getContentPane().setLayout(new FlowLayout());
        getContentPane().add(statusLabel);
        getContentPane().add(statusButton);
    }


    public void setStatus(boolean newStatus) {
        state = newStatus;
        statusButton.setSelected(state);
        statusButton.setText((state?"ON":"OFF"));
    }
}
