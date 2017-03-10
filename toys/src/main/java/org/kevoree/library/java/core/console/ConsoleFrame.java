package org.kevoree.library.java.core.console;

import javax.swing.*;

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
