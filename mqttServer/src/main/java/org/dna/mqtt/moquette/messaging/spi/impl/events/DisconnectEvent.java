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
package org.dna.mqtt.moquette.messaging.spi.impl.events;

import org.dna.mqtt.moquette.server.ServerChannel;

/**
 *
 * @author andrea
 */
public class DisconnectEvent extends MessagingEvent {
    
    ServerChannel m_session;
    
    public DisconnectEvent(ServerChannel session) {
        m_session = session;
    }

    public ServerChannel getSession() {
        return m_session;
    }
    
    
}