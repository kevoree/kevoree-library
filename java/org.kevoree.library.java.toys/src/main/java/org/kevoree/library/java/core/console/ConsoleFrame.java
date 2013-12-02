package org.kevoree.library.java.core.console;

import org.kevoree.library.java.toys.ToyConsole;
import org.kevoree.log.Log;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 02/12/2013
 * Time: 10:49
 * To change this template use File | Settings | File Templates.
 */
public class ConsoleFrame extends JFrame {


    public ConsoleFrame(String name) {
        super(name);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }


    public void init(final JPanel p) {

        setContentPane(p);
        pack();
        setVisible(true);

    }


}
