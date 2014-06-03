/*
package org.kevoree.library.xmpp.cmp;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.kevoree.annotation.*;
import org.kevoree.api.ChannelContext;
import org.kevoree.api.ChannelDispatch;
import org.kevoree.log.Log;

@Library(name = "Java :: XMPP")
@ChannelType
@ThirdParties({
        @ThirdParty(name = "org.kevoree.extra.marshalling", url = "mvn:org.kevoree.extra/org.kevoree.extra.marshalling")
})
public class XmppChannel implements ConnectionListener, ChannelDispatch {

    @Param(optional = false)
    protected String userName;

    @Param(optional = false)
    protected String password;

    @KevoreeInject
    protected ChannelContext context;

    ConnectionConfiguration config = null;
    XMPPConnection connection = null;


    @Start
    public void startChannel() {
        try {
            config = new ConnectionConfiguration("talk.google.com", 5222, "Work");
            connection = new XMPPConnection(config);
            connection.connect();
            connection.login(userName, password);
            connection.addConnectionListener(this);
            connection.sendPacket(new Presence(Presence.Type.available));

            PacketTypeFilter filter = new PacketTypeFilter(org.jivesoftware.smack.packet.Message.class);
            PacketListener myListener = new PacketListener() {

                public void processPacket(Packet packet) {
                    String name = packet.getFrom().substring(0, packet.getFrom().indexOf("/"));

                    Log.debug("rec" + name + "=" + packet.getPropertyNames());
                }
            };
            connection.addPacketListener(myListener, filter);


        } catch (Exception e) {
//            e.printStackTrace();
            Log.error("", e);
        }


    }

    @Stop
    public void stopChannel() {
        connection.disconnect();
    }

    @Override
    public Object dispatch(Message message) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ChannelFragmentSender createSender(String s, String s1) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void connectionClosed() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void reconnectingIn(int i) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void reconnectionSuccessful() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void reconnectionFailed(Exception e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
*/