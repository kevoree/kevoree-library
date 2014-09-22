package org.kevoree.library.java.network;

import org.kevoree.log.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * Created by duke on 19/02/2014.
 */
public class UDPWrapper implements Runnable {

    public UDPWrapper(Integer port) throws SocketException {
        Log.info("Admin Control Port : {}", port);
        serverSocket = new DatagramSocket(port);
    }

    byte[] receiveData = new byte[1024];
    DatagramSocket serverSocket;

    @Override
    public void run() {
        try {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            while (!Thread.currentThread().isInterrupted()) {
                serverSocket.receive(receivePacket);
                String sentence = new String(receivePacket.getData());
                if (sentence.toLowerCase().trim().contains("stop")) {
                    System.setSecurityManager(null);
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
