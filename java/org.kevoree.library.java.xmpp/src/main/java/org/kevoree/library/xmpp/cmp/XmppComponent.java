/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kevoree.library.xmpp.cmp;

import org.kevoree.api.Port;
import org.kevoree.annotation.*;
import org.kevoree.library.xmpp.mngr.ConnectionManager;
import org.kevoree.log.Log;

import java.util.Properties;

/**
 *
 * @author gnain
 */

@ComponentType(version = 1)
public class XmppComponent {

    private ConnectionManager client;

    @Param(optional = false)
    protected String login;

    @Param(optional = false)
    protected String password;

    @Output
    protected Port messageReceived;


    @Input
    public void send(Object message) {
        Log.debug("XMPP Send msg =>" + message.toString());
        Properties msg = (Properties)message;
        System.out.println("Sending message to: " + msg.get("to"));
        System.out.println(msg.get("content"));

   //     client.sendMessage((String)msg.get("content"), (String)msg.get("to"), defaultListener);
        System.out.println(msg.get("Sent"));
    }

    public void messageReceived(String message) {

        if(messageReceived.getConnectedBindingsSize() >0) {
            messageReceived.send(message);
        }
    }

    @Start
    public void start() {
        Log.info("Starting");
        Log.debug("Credentials: '" + login + "':'" + password + "'");
        client = new ConnectionManager();
        if(client.login(login, password)) {
  //      defaultListener = new LocalMessageListener(this);
   //     client.setDefaultResponseStrategy(defaultListener);
            Log.info("Started");
        } else {
            Log.warn("Error while connecting to the server.");
        }

    }

    @Stop
    public void stop() {
        Log.info("Closing...");
        /*
        System.out.print("Current contact list:\n");
        for(RosterEntry entry : client.getContacts()) {
            System.out.print("\t" + entry.getUser() + " as " + entry.getName() + " is "+entry.getStatus() + "\n");
            //System.out.print("\t"+entry.toString()+"\n");
        }
        System.out.println();
         */
        client.disconnect();
        Log.info("Closed.");
    }

}
