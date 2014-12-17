package org.kevoree.library;

import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.Port;
import org.kevoree.library.java.core.console.ConsoleFrame;
import org.kevoree.library.java.core.console.ConsolePanel;
import org.kevoree.library.java.core.console.TabbedConsoleFrame;
import org.kevoree.log.Log;

import javax.swing.*;

/**
 * Offers a Graphical frame where input text is displayed and where text can also be typed in.
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 02/12/2013
 * Time: 10:47
 */
@ComponentType
public class ToyConsole {


    private ConsolePanel thisConsole;
    private ConsoleFrame standaloneFrame;

    @Param(defaultValue = "true")
    protected Boolean showInTab = true;

    @Output
    protected Port textEntered;

    @KevoreeInject
    protected Context cmpContext;

    /**
     * Uniquely identifies the console in tabs panels. Also used as title for the Standalone frame.
     */
    private String consoleKey;

    public ToyConsole() {
    }


    @Start
    public void startConsole() {

        //Uniquely identifies the console
        consoleKey = cmpContext.getInstanceName() + "@" + cmpContext.getNodeName();
        thisConsole = new ConsolePanel(this);
        thisConsole.appendSystem("/***** CONSOLE READY ******/ ");

        if(showInTab) {
            TabbedConsoleFrame.getInstance().addTab(thisConsole, consoleKey);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    standaloneFrame = new ConsoleFrame(consoleKey);
                    standaloneFrame.init(thisConsole);
                }
            });
        }
    }

    @Stop
    public void stop() {
        if(showInTab) {
            TabbedConsoleFrame.getInstance().releaseTab(consoleKey);
            Log.debug("Stopped from TAB");
        } else {
            if(standaloneFrame != null) {
                standaloneFrame.dispose();
            }
        }
        standaloneFrame = null;
        thisConsole = null;
        consoleKey = null;
    }


    @Update
    public void update() {
        if(showInTab) {
            if(standaloneFrame != null) {
                standaloneFrame.dispose();
                standaloneFrame = null;
                consoleKey = cmpContext.getInstanceName() + "@" + cmpContext.getNodeName();
                TabbedConsoleFrame.getInstance().addTab(thisConsole, consoleKey);
            }
        } else {
            if(standaloneFrame == null) {
                TabbedConsoleFrame.getInstance().releaseTab(consoleKey);
                consoleKey = cmpContext.getInstanceName() + "@" + cmpContext.getNodeName();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        standaloneFrame = new ConsoleFrame(consoleKey);
                        standaloneFrame.init(thisConsole);
                    }
                });
            }
        }
    }

    public void appendSystem(String text) {
        thisConsole.appendSystem(text);
    }

    public void textTypedLocally(String text) {
        textEntered.send(text,null);
    }

    @Input
    public void showText(Object text) {
        if (text != null) {
            thisConsole.appendIncomming("->" + text.toString());
        }
    }

}
