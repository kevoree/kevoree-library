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
@ComponentType(version=2)
public class ToyConsole {

    private ConsolePanel console;
    private ConsoleFrame standaloneFrame;

    @Param(defaultValue = "true")
    private Boolean showInTab = true;

    @Output
    private Port output;

    @KevoreeInject
    private Context cmpContext;

    /**
     * Uniquely identifies the console in tabs panels. Also used as title for the Standalone frame.
     */
    private String consoleKey;

    @Start
    public void startConsole() {
        //Uniquely identifies the console
        consoleKey = cmpContext.getInstanceName() + "@" + cmpContext.getNodeName();
        console = new ConsolePanel(this);
        console.appendSystem("/***** CONSOLE READY ******/ ");

        if(showInTab) {
            TabbedConsoleFrame.getInstance().addTab(console, consoleKey);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    standaloneFrame = new ConsoleFrame(consoleKey);
                    standaloneFrame.init(console);
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
        console = null;
        consoleKey = null;
    }


    @Update
    public void update() {
        if(showInTab) {
            if(standaloneFrame != null) {
                standaloneFrame.dispose();
                standaloneFrame = null;
                consoleKey = cmpContext.getInstanceName() + "@" + cmpContext.getNodeName();
                TabbedConsoleFrame.getInstance().addTab(console, consoleKey);
            }
        } else {
            if(standaloneFrame == null) {
                TabbedConsoleFrame.getInstance().releaseTab(consoleKey);
                consoleKey = cmpContext.getInstanceName() + "@" + cmpContext.getNodeName();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        standaloneFrame = new ConsoleFrame(consoleKey);
                        standaloneFrame.init(console);
                    }
                });
            }
        }
    }

    public void appendSystem(String text) {
        console.appendSystem(text);
    }

    public void textTypedLocally(String text) {
        output.send(text,null);
    }

    @Input
    public void input(String msg) {
        if (msg != null) {
            console.appendIncomming(">" + msg);
        }
    }
}
