package org.kevoree.library.java.core.control;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 02/12/2013
 * Time: 11:58
 */
public class OnOffSwitchFrame extends JFrame {

    private JButton btn_on, btn_off, btn_toggle;
    private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();

    public OnOffSwitchFrame(String name) {
        super(name);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        initGraphicalComponents();
        layoutGraphicalComponents();

        pack();
    }

    public void addActionListener(ActionListener al) {
        btn_on.addActionListener(al);
        btn_off.addActionListener(al);
        btn_toggle.addActionListener(al);
    }

    public void removeActionListener(ActionListener al) {
        btn_on.removeActionListener(al);
        btn_off.removeActionListener(al);
        btn_toggle.removeActionListener(al);
    }

    private void initGraphicalComponents() {
        btn_on = new JButton("ON");
        btn_on.setActionCommand("ON");

        btn_off = new JButton("OFF");
        btn_off.setActionCommand("OFF");

        btn_toggle = new JButton("TOGGLE");
        btn_toggle.setActionCommand("TOGGLE");
    }

    private void layoutGraphicalComponents() {
        getContentPane().setLayout(new FlowLayout());
        getContentPane().add(btn_on);
        getContentPane().add(btn_off);
        getContentPane().add(btn_toggle);
        setPreferredSize(new Dimension(110,130));
    }

}
