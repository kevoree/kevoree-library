/*
 * Copyright (c) 2012-2014 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package org.dna.mqtt.moquette.messaging.spi.impl;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.dna.mqtt.moquette.messaging.spi.IMatchingCondition;
import org.dna.mqtt.moquette.messaging.spi.IStorageService;
import org.dna.mqtt.moquette.messaging.spi.impl.events.MessagingEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.OutputMessagingEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PubAckEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PublishEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.Subscription;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.SubscriptionsStore;
import org.dna.mqtt.moquette.proto.messages.PubCompMessage;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.dna.mqtt.moquette.proto.messages.PubAckMessage;
import org.dna.mqtt.moquette.proto.messages.PubRecMessage;
import org.dna.mqtt.moquette.proto.messages.PubRelMessage;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;
import org.dna.mqtt.moquette.proto.messages.SubAckMessage;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage;
import org.dna.mqtt.moquette.proto.messages.UnsubAckMessage;
import org.dna.mqtt.moquette.server.ConnectionDescriptor;
import org.dna.mqtt.moquette.server.Constants;
import org.dna.mqtt.moquette.server.IAuthenticator;
import org.dna.mqtt.moquette.server.ServerChannel;
import org.kevoree.log.Log;

/**
 * Class responsible to handle the logic of MQTT protocol it's the director of
 * the protocol execution. 
 *
 * Used by the front facing class SimpleMessaging.
 *
 * @author andrea
 */
class ProtocolProcessor implements EventHandler<ValueEvent> {

    static final class WillMessage {
        private final String topic;
        private final ByteBuffer payload;
        private final boolean retained;
        private final QOSType qos;

        public WillMessage(String topic, ByteBuffer payload, boolean retained, QOSType qos) {
            this.topic = topic;
            this.payload = payload;
            this.retained = retained;
            this.qos = qos;
        }

        public String getTopic() {
            return topic;
        }

        public ByteBuffer getPayload() {
            return payload;
        }

        public boolean isRetained() {
            return retained;
        }

        public QOSType getQos() {
            return qos;
        }

    }



    private Map<String, ConnectionDescriptor> m_clientIDs = new HashMap<String, ConnectionDescriptor>();
    private SubscriptionsStore subscriptions;
    private IStorageService m_storageService;
    private IAuthenticator m_authenticator;
    //maps clientID to Will testament, if specified on CONNECT
    private Map<String, WillMessage> m_willStore = new HashMap<String, WillMessage>();

    private ExecutorService m_executor;
    BatchEventProcessor<ValueEvent> m_eventProcessor;
    private RingBuffer<ValueEvent> m_ringBuffer;

    ProtocolProcessor() {}

    /**
     * @param subscriptions the subscription store where are stored all the existing
     *  clients subscriptions.
     * @param storageService the persistent store to use for save/load of messages
     *  for QoS1 and QoS2 handling.
     * @param authenticator the authenticator used in connect messages
     */
    void init(SubscriptionsStore subscriptions, IStorageService storageService,
              IAuthenticator authenticator) {
        //m_clientIDs = clientIDs;
        this.subscriptions = subscriptions;
        m_authenticator = authenticator;
        m_storageService = storageService;

        //init the output ringbuffer
        m_executor = Executors.newFixedThreadPool(1);

        m_ringBuffer = new RingBuffer<ValueEvent>(ValueEvent.EVENT_FACTORY, 1024 * 32);

        SequenceBarrier barrier = m_ringBuffer.newBarrier();
        m_eventProcessor = new BatchEventProcessor<ValueEvent>(m_ringBuffer, barrier, this);
        //TODO in a presentation is said to don't do the followinf line!!
        m_ringBuffer.setGatingSequences(m_eventProcessor.getSequence());
        m_executor.submit(m_eventProcessor);
    }

    void processConnect(ServerChannel session, ConnectMessage msg) {
        Log.trace("processConnect for client {}", msg.getClientID());
        if (msg.getProcotolVersion() != 0x03) {
            ConnAckMessage badProto = new ConnAckMessage();
            badProto.setReturnCode(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION);
            Log.warn("processConnect sent bad proto ConnAck");
            session.write(badProto);
            session.close(false);
            return;
        }

        if (msg.getClientID() == null || msg.getClientID().length() > 23) {
            ConnAckMessage okResp = new ConnAckMessage();
            okResp.setReturnCode(ConnAckMessage.IDENTIFIER_REJECTED);
            session.write(okResp);
            return;
        }

        //if an old client with the same ID already exists close its session.
        if (m_clientIDs.containsKey(msg.getClientID())) {
            //clean the subscriptions if the old used a cleanSession = true
            ServerChannel oldSession = m_clientIDs.get(msg.getClientID()).getSession();
            boolean cleanSession = (Boolean) oldSession.getAttribute(Constants.CLEAN_SESSION);
            if (cleanSession) {
                //cleanup topic subscriptions
                processRemoveAllSubscriptions(msg.getClientID());
            }

            m_clientIDs.get(msg.getClientID()).getSession().close(false);
        }

        ConnectionDescriptor connDescr = new ConnectionDescriptor(msg.getClientID(), session, msg.isCleanSession());
        m_clientIDs.put(msg.getClientID(), connDescr);

        int keepAlive = msg.getKeepAlive();
        Log.trace("Connect with keepAlive {} s",  keepAlive);
        session.setAttribute(Constants.KEEP_ALIVE, keepAlive);
        session.setAttribute(Constants.CLEAN_SESSION, msg.isCleanSession());
        //used to track the client in the subscription and publishing phases.
        session.setAttribute(Constants.ATTR_CLIENTID, msg.getClientID());

        session.setIdleTime(Math.round(keepAlive * 1.5f));

        //Handle will flag
        if (msg.isWillFlag()) {
            AbstractMessage.QOSType willQos = AbstractMessage.QOSType.values()[msg.getWillQos()];
            byte[] willPayload = msg.getWillMessage().getBytes();
            ByteBuffer bb = (ByteBuffer) ByteBuffer.allocate(willPayload.length).put(willPayload).flip();
            //save the will testment in the clientID store
            WillMessage will = new WillMessage(msg.getWillTopic(), bb, msg.isWillRetain(),willQos );
            m_willStore.put(msg.getClientID(), will);
        }

        //handle user authentication
        if (msg.isUserFlag()) {
            String pwd = null;
            if (msg.isPasswordFlag()) {
                pwd = msg.getPassword();
            }
            if (!m_authenticator.checkValid(msg.getUsername(), pwd)) {
                ConnAckMessage okResp = new ConnAckMessage();
                okResp.setReturnCode(ConnAckMessage.BAD_USERNAME_OR_PASSWORD);
                session.write(okResp);
                return;
            }
        }

        subscriptions.activate(msg.getClientID());

        //handle clean session flag
        if (msg.isCleanSession()) {
            //remove all prev subscriptions
            //cleanup topic subscriptions
            processRemoveAllSubscriptions(msg.getClientID());
        }

        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);
        Log.trace("processConnect sent OK ConnAck");
        session.write(okResp);
        Log.info("Connected client ID <{}> with clean session {}", msg.getClientID(), msg.isCleanSession());

        if (!msg.isCleanSession()) {
            //force the republish of stored QoS1 and QoS2
            republishStored(msg.getClientID());
        }
    }

    private void republishStored(String clientID) {
        Log.trace("republishStored invoked");
        List<PublishEvent> publishedEvents = m_storageService.retrievePersistedPublishes(clientID);
        if (publishedEvents == null) {
            Log.debug("No stored messages for client <{}>", clientID);
            return;
        }

        Log.trace("republishing stored messages to client <{}>", clientID);
        for (PublishEvent pubEvt : publishedEvents) {
            sendPublish(pubEvt.getClientID(), pubEvt.getTopic(), pubEvt.getQos(),
                    pubEvt.getMessage(), false, pubEvt.getMessageID());
        }
    }

    void processPubAck(String clientID, int messageID) {
        //Remove the message from message store
        m_storageService.cleanPersistedPublishMessage(clientID, messageID);
    }

    private void processRemoveAllSubscriptions(String clientID) {
        Log.trace("cleaning old saved subscriptions for client <{}>", clientID);
        subscriptions.removeForClient(clientID);

        //remove also the messages stored of type QoS1/2
        m_storageService.cleanPersistedPublishes(clientID);
    }

    protected void processPublish(PublishEvent evt) {
        Log.trace("PUB --PUBLISH--> SRV processPublish invoked with {}", evt);
        final String topic = evt.getTopic();
        final AbstractMessage.QOSType qos = evt.getQos();
        final ByteBuffer message = evt.getMessage();
        boolean retain = evt.isRetain();

        String publishKey = null;
        if (qos == AbstractMessage.QOSType.LEAST_ONE) {
            //store the temporary message
            publishKey = String.format("%s%d", evt.getClientID(), evt.getMessageID());
            m_storageService.addInFlight(evt, publishKey);
        }  else if (qos == AbstractMessage.QOSType.EXACTLY_ONCE) {
            publishKey = String.format("%s%d", evt.getClientID(), evt.getMessageID());
            //store the message in temp store
            m_storageService.persistQoS2Message(publishKey, evt);
            sendPubRec(evt.getClientID(), evt.getMessageID());
        }

        //NB publish to subscribers for QoS 2 happen upon PUBREL from publisher
        if (qos != AbstractMessage.QOSType.EXACTLY_ONCE) {
            publish2Subscribers(topic, qos, message, retain, evt.getMessageID());
        }

        if (qos == AbstractMessage.QOSType.LEAST_ONE) {
            if (publishKey == null) {
                throw new RuntimeException("Found a publish key null for QoS " + qos + " for message " + evt);
            }
            m_storageService.cleanInFlight(publishKey);
            sendPubAck(new PubAckEvent(evt.getMessageID(), evt.getClientID()));
            Log.trace("replying with PubAck to MSG ID {}", evt.getMessageID());
        }

        if (retain) {
            m_storageService.storeRetained(topic, message, qos);
        }
    }

    /**
     * Flood the subscribers with the message to notify. MessageID is optional and should only used for QoS 1 and 2
     * */
    private void publish2Subscribers(String topic, AbstractMessage.QOSType qos, ByteBuffer origMessage, boolean retain, Integer messageID) {
        Log.trace("publish2Subscribers republishing to existing subscribers that matches the topic {}", topic);
        if (Log.DEBUG) {
            Log.trace("content <{}>", DebugUtils.payload2Str(origMessage));
            Log.trace("subscription tree {}", subscriptions.dumpTree());
        }
        for (final Subscription sub : subscriptions.matches(topic)) {
            if(m_clientIDs.get(sub.getClientId()) == null) {
                subscriptions.removeSubscription(topic, sub.getClientId());
                continue;
            }
            if (qos.ordinal() > sub.getRequestedQos().ordinal()) {
                qos = sub.getRequestedQos();
            }

            ByteBuffer message = origMessage.duplicate();

            if (qos == AbstractMessage.QOSType.MOST_ONE && sub.isActive()) {
                //QoS 0
                sendPublish(sub.getClientId(), topic, qos, message, false);
            } else {
                //QoS 1 or 2
                //if the target subscription is not clean session and is not connected => store it
                if (!sub.isCleanSession() && !sub.isActive()) {
                    //clone the event with matching clientID
                    PublishEvent newPublishEvt = new PublishEvent(topic, qos, message, retain, sub.getClientId(), messageID);
                    m_storageService.storePublishForFuture(newPublishEvt);
                } else  {
                    //if QoS 2 then store it in temp memory
                    if (qos ==AbstractMessage.QOSType.EXACTLY_ONCE) {
                        String publishKey = String.format("%s%d", sub.getClientId(), messageID);
                        PublishEvent newPublishEvt = new PublishEvent(topic, qos, message, retain, sub.getClientId(), messageID);
                        m_storageService.addInFlight(newPublishEvt, publishKey);
                    }
                    //publish
                    if (sub.isActive()) {
                        sendPublish(sub.getClientId(), topic, qos, message, false);
                    }
                }
            }
        }
    }

    private void sendPublish(String clientId, String topic, AbstractMessage.QOSType qos, ByteBuffer message, boolean retained) {
        //TODO pay attention to the message ID can't be 0 and it's the message sent to subscriber
        int messageID = 1;
        sendPublish(clientId, topic, qos, message, retained, messageID);
    }

    private void sendPublish(String clientId, String topic, AbstractMessage.QOSType qos, ByteBuffer message, boolean retained, int messageID) {
        PublishMessage pubMessage = new PublishMessage();
        pubMessage.setRetainFlag(retained);
        pubMessage.setTopicName(topic);
        pubMessage.setQos(qos);
        pubMessage.setPayload(message);

        Log.trace("send publish message to <{}> on topic <{}>", clientId, topic);
        if (Log.DEBUG) {
            Log.trace("content <{}>", DebugUtils.payload2Str(message));
        }
        if (pubMessage.getQos() != AbstractMessage.QOSType.MOST_ONE) {
            pubMessage.setMessageID(messageID);
        }

        if (m_clientIDs == null) {
            throw new RuntimeException("Internal bad error, found m_clientIDs to null while it should be initialized, somewhere it's overwritten!!");
        }
        Log.trace("clientIDs are {}", m_clientIDs);
        if (m_clientIDs.get(clientId) == null) {
            throw new RuntimeException(String.format("Can't find a ConnectionDescriptor for client <%s> in cache <%s>", clientId, m_clientIDs));
        }
        Log.trace("Session for clientId {} is {}", clientId, m_clientIDs.get(clientId).getSession());
//            m_clientIDs.get(clientId).getSession().write(pubMessage);
        disruptorPublish(new OutputMessagingEvent(m_clientIDs.get(clientId).getSession(), pubMessage));
    }

    private void sendPubRec(String clientID, int messageID) {
        Log.trace("PUB <--PUBREC-- SRV sendPubRec invoked for clientID {} with messageID {}", clientID, messageID);
        PubRecMessage pubRecMessage = new PubRecMessage();
        pubRecMessage.setMessageID(messageID);

//        m_clientIDs.get(clientID).getSession().write(pubRecMessage);
        disruptorPublish(new OutputMessagingEvent(m_clientIDs.get(clientID).getSession(), pubRecMessage));
    }

    private void sendPubAck(PubAckEvent evt) {
        Log.trace("sendPubAck invoked");

        String clientId = evt.getClientID();

        PubAckMessage pubAckMessage = new PubAckMessage();
        pubAckMessage.setMessageID(evt.getMessageId());

        if (m_clientIDs != null && m_clientIDs.get(clientId)!= null) {

            Log.trace("clientIDs are {}", m_clientIDs);
            Log.debug("Session for clientId " + clientId + " is " + m_clientIDs.get(clientId).getSession());

            disruptorPublish(new OutputMessagingEvent(m_clientIDs.get(clientId).getSession(), pubAckMessage));
        }
    }

    /**
     * Second phase of a publish QoS2 protocol, sent by publisher to the broker. Search the stored message and publish
     * to all interested subscribers.
     * */
    void processPubRel(String clientID, int messageID) {
        Log.trace("PUB --PUBREL--> SRV processPubRel invoked for clientID {} ad messageID {}", clientID, messageID);
        String publishKey = String.format("%s%d", clientID, messageID);
        PublishEvent evt = m_storageService.retrieveQoS2Message(publishKey);

        final String topic = evt.getTopic();
        final AbstractMessage.QOSType qos = evt.getQos();

        publish2Subscribers(topic, qos, evt.getMessage(), evt.isRetain(), evt.getMessageID());

        m_storageService.removeQoS2Message(publishKey);

        if (evt.isRetain()) {
            m_storageService.storeRetained(topic, evt.getMessage(), qos);
        }

        sendPubComp(clientID, messageID);
    }

    private void sendPubComp(String clientID, int messageID) {
        Log.trace("PUB <--PUBCOMP-- SRV sendPubComp invoked for clientID {} ad messageID {}", clientID, messageID);
        PubCompMessage pubCompMessage = new PubCompMessage();
        pubCompMessage.setMessageID(messageID);

//        m_clientIDs.get(clientID).getSession().write(pubCompMessage);
        disruptorPublish(new OutputMessagingEvent(m_clientIDs.get(clientID).getSession(), pubCompMessage));
    }

    void processPubRec(String clientID, int messageID) {
        //once received a PUBREC reply with a PUBREL(messageID)
        Log.trace("\t\tSRV <--PUBREC-- SUB processPubRec invoked for clientID {} ad messageID {}", clientID, messageID);
        PubRelMessage pubRelMessage = new PubRelMessage();
        pubRelMessage.setMessageID(messageID);
        pubRelMessage.setQos(AbstractMessage.QOSType.LEAST_ONE);

//        m_clientIDs.get(clientID).getSession().write(pubRelMessage);
        disruptorPublish(new OutputMessagingEvent(m_clientIDs.get(clientID).getSession(), pubRelMessage));
    }

    void processPubComp(String clientID, int messageID) {
        Log.trace("\t\tSRV <--PUBCOMP-- SUB processPubComp invoked for clientID {} ad messageID {}", clientID, messageID);
        //once received the PUBCOMP then remove the message from the temp memory
        String publishKey = String.format("%s%d", clientID, messageID);
        m_storageService.cleanInFlight(publishKey);
    }

    void processDisconnect(ServerChannel session, String clientID, boolean cleanSession) throws InterruptedException {
        if (cleanSession) {
            //cleanup topic subscriptions
            processRemoveAllSubscriptions(clientID);
        }
//        m_notifier.disconnect(evt.getSession());
        m_clientIDs.remove(clientID);
        session.close(true);

        //de-activate the subscriptions for this ClientID
        subscriptions.deactivate(clientID);
        //cleanup the will store
        m_willStore.remove(clientID);

        Log.info("Disconnected client <{}> with clean session {}", clientID, cleanSession);
    }

    void proccessConnectionLost(String clientID) {
        //If already removed a disconnect message was already processed for this clientID
        if (m_clientIDs.remove(clientID) != null) {

            //de-activate the subscriptions for this ClientID
            subscriptions.deactivate(clientID);
            Log.info("Lost connection with client <{}>", clientID);
        }
        //TODO publish the Will message (if any) for the clientID
        if (m_willStore.containsKey(clientID)) {
            WillMessage will = m_willStore.get(clientID);
            PublishEvent pubEvt = new PublishEvent(will.getTopic(), will.getQos(),
                    will.getPayload(), will.isRetained(), clientID);
            processPublish(pubEvt);
            m_willStore.remove(clientID);
        }
    }

    /**
     * Remove the clientID from topic subscription, if not previously subscribed,
     * doesn't reply any error
     */
    void processUnsubscribe(ServerChannel session, String clientID, List<String> topics, int messageID) {
        Log.trace("processUnsubscribe invoked, removing subscription on topics {}, for clientID <{}>", topics, clientID);

        for (String topic : topics) {
            subscriptions.removeSubscription(topic, clientID);
        }
        //ack the client
        UnsubAckMessage ackMessage = new UnsubAckMessage();
        ackMessage.setMessageID(messageID);

        Log.trace("replying with UnsubAck to MSG ID {}", messageID);
        session.write(ackMessage);
    }


    void processSubscribe(ServerChannel session, SubscribeMessage msg, String clientID, boolean cleanSession) {
        Log.info("processSubscribe invoked from client {} with msgID {}", clientID, msg.getMessageID());

        for (SubscribeMessage.Couple req : msg.subscriptions()) {
            AbstractMessage.QOSType qos = AbstractMessage.QOSType.values()[req.getQos()];
            Subscription newSubscription = new Subscription(clientID, req.getTopic(), qos, cleanSession);
            subscribeSingleTopic(newSubscription, req.getTopic());
        }

        //ack the client
        SubAckMessage ackMessage = new SubAckMessage();
        ackMessage.setMessageID(msg.getMessageID());

        //reply with requested qos
        for(SubscribeMessage.Couple req : msg.subscriptions()) {
            AbstractMessage.QOSType qos = AbstractMessage.QOSType.values()[req.getQos()];
            ackMessage.addType(qos);
        }

        Log.trace("replying with SubAck to MSG ID {}", msg.getMessageID());
        session.write(ackMessage);
    }

    private void subscribeSingleTopic(Subscription newSubscription, final String topic) {
        subscriptions.add(newSubscription);

        //scans retained messages to be published to the new subscription
        Collection<HawtDBStorageService.StoredMessage> messages = m_storageService.searchMatching(new IMatchingCondition() {
            public boolean match(String key) {
                return  SubscriptionsStore.matchTopics(key, topic);
            }
        });

        for (HawtDBStorageService.StoredMessage storedMsg : messages) {
            //fire the as retained the message
            Log.trace("send publish message for topic {}", topic);
            sendPublish(newSubscription.getClientId(), storedMsg.getTopic(), storedMsg.getQos(), storedMsg.getPayload(), true);
        }
    }

    private void disruptorPublish(OutputMessagingEvent msgEvent) {
        Log.trace("disruptorPublish publishing event on output {}", msgEvent);
        long sequence = m_ringBuffer.next();
        ValueEvent event = m_ringBuffer.get(sequence);

        event.setEvent(msgEvent);

        m_ringBuffer.publish(sequence);
    }

    public void onEvent(ValueEvent t, long l, boolean bln) throws Exception {
        MessagingEvent evt = t.getEvent();
        //It's always of type OutputMessagingEvent
        OutputMessagingEvent outEvent = (OutputMessagingEvent) evt;
        outEvent.getChannel().write(outEvent.getMessage());
    }

}
