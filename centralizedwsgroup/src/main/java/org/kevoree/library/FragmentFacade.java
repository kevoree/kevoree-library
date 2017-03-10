package org.kevoree.library;

/**
 * Hides away the complexity of ServerAdapter/ClientAdapter
 * In this project, CentralizedWSGroup does not need to know more
 * than how to start() and close() its fragment, whether it is a
 * WebSocket client or a WebSocket server.
 *
 * Created by leiko on 1/10/17.
 */
public interface FragmentFacade {

    void start();
    void close();
}
