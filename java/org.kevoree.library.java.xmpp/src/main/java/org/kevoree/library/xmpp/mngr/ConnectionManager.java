package org.kevoree.library.xmpp.mngr;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.kevoree.library.xmpp.core.XmppKernel;
import org.kevoree.log.Log;

import java.io.File;
import java.util.*;

public class ConnectionManager implements ConnectionListener {

    private XmppKernel kernel;


    public ConnectionManager() {
        kernel = new XmppKernel();
        kernel.setConnectionManager(this);
        new ChatManager(kernel);
    }



    public boolean login(String userName, String password) {
        try {

            ConnectionConfiguration config = new ConnectionConfiguration("talk.google.com", 5222, "Work");

            kernel.setXmppConnection(new XMPPConnection(config));

            new ContactsManager(kernel);

            kernel.getXmppConnection().connect();

            kernel.getXmppConnection().addConnectionListener(this);

            kernel.getXmppConnection().login(userName, password);

            kernel.getXmppConnection().getRoster().addRosterListener(kernel.getContactsManager());

            return true;

        } catch (XMPPException e) {
            e.printStackTrace();
            Log.error("Log in failed", e);
            return false;
        }
    }

    public void setAvailability(boolean available) {
        if(available) {
            kernel.getXmppConnection().sendPacket(new Presence(Presence.Type.available));
        } else {
            kernel.getXmppConnection().sendPacket(new Presence(Presence.Type.unavailable));
        }
    }

    public Collection<RosterEntry> getContacts() {
        return kernel.getContactsManager().getContactList();
    }

    public boolean removeContact(String contactAddress) {
        return kernel.getContactsManager().removeRecipient(contactAddress);
    }

    public boolean addContact(String contactAddress) {
        return kernel.getContactsManager().addRecipient(contactAddress);
    }

    public void sendMessage(String message, String to, MessageListener listener) {
        kernel.getChatsManager().sendMessage(message, to, listener);
    }

    public void disconnect() {
        kernel.getXmppConnection().sendPacket(new Presence(Presence.Type.unavailable));
        kernel.getXmppConnection().disconnect();
    }

    public void printContactsStats() {
        kernel.getContactsManager().printContactsStats();
    }

    /*
    public void setDefaultResponseStrategy(final MessageListener messageList) {

        PacketTypeFilter filter = new PacketTypeFilter(Message.class);
        PacketListener myListener = new PacketListener() {

            public void processPacket(Packet packet) {
                String name = packet.getFrom().substring(0, packet.getFrom().indexOf("/"));

            }
        };
        connection.addPacketListener(myListener, filter);
    }
*/


    public void sendFile(File f, String description, String to) {
        //kernel.getChatsManager().sendFile(f,description,to);
    }

    public void connectionClosed() {
        Log.debug("XMPP Connection Closed");
    }

    @Override
    public void connectionClosedOnError(Exception excptn) {
//        excptn.printStackTrace();
        Log.error("Connection closed on error", excptn);
    }

    @Override
    public void reconnectingIn(int i) {
        Log.debug("XMPP Reconnecting(" + i + ")...");
    }

    @Override
    public void reconnectionSuccessful() {
        Log.debug("XMPP Re-Connection Ok");
    }

    @Override
    public void reconnectionFailed(Exception excptn) {
//        excptn.printStackTrace();
        Log.error("Reconnection failed", excptn);

    }

}
