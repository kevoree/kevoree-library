package org.kevoree.library.java.toys;

import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.Port;
import org.kevoree.library.java.core.console.ConsoleFrame;
import org.kevoree.library.java.core.console.ConsolePanel;
import org.kevoree.library.java.core.console.TabbedConsoleFrame;

/**
 * Created with IntelliJ IDEA.
 * User: gregory.nain
 * Date: 02/12/2013
 * Time: 10:47
 */
@ComponentType
@Library(name="Java :: Toys")
public class ToyConsole {


    private ConsolePanel thisConsole;
    private ConsoleFrame standaloneFrame;

    @Param
    protected Boolean showInTab = true;

    @Output
    protected Port textEntered;

    @KevoreeInject
    protected Context cmpContext;

    @Start
    public void startConsole() {
        thisConsole = new ConsolePanel(this);
        // frame.setTitle(getName() + "@@@" + getNodeName());
        //  frame.setVisible(true);
        thisConsole.appendSystem("/***** CONSOLE INITIALIZED  ********/ ");
        thisConsole.appendSystem("//DEBUG " + cmpContext.getInstanceName() + "@" + cmpContext.getNodeName() + ":" + this.toString() + ":" + cmpContext.hashCode());
        if(showInTab) {
            TabbedConsoleFrame.getInstance().addTab(thisConsole, cmpContext.getInstanceName() + "@" + cmpContext.getNodeName() + ":" + this.toString());
        } else {
            standaloneFrame = new ConsoleFrame(cmpContext.getInstanceName() + "@" + cmpContext.getNodeName());
            standaloneFrame.init(thisConsole);
        }
    }

    @Stop
    public void stop() {
        if(showInTab) {
            TabbedConsoleFrame.getInstance().releaseTab(cmpContext.getInstanceName() + "@" + cmpContext.getNodeName() + ":" + this.toString());
        } else {
            if(standaloneFrame != null) {
                standaloneFrame.dispose();
            }
        }
        standaloneFrame = null;
        thisConsole = null;
    }


    @Update
    public void update() {
        if(showInTab) {
            if(standaloneFrame != null) {
                TabbedConsoleFrame.getInstance().addTab(thisConsole, cmpContext.getInstanceName() + "@" + cmpContext.getNodeName() + ":" + this.toString());
                standaloneFrame.dispose();
                standaloneFrame = null;
            }
        } else {
            if(standaloneFrame == null) {
                TabbedConsoleFrame.getInstance().releaseTab(cmpContext.getInstanceName() + "@" + cmpContext.getNodeName() + ":" + this.toString());
                standaloneFrame = new ConsoleFrame(cmpContext.getInstanceName() + "@" + cmpContext.getNodeName());
                standaloneFrame.init(thisConsole);
            }
        }
    }

    public void appendSystem(String text) {
        thisConsole.appendSystem(text);
    }

    public void textTypedLocally(String text) {
        textEntered.call(text);
    }

    @Input
    public void showText(Object text) {
        if (text != null) {
            thisConsole.appendIncomming("->" + text.toString());
        }
    }

}
