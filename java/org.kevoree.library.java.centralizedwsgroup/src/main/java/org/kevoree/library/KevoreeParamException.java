package org.kevoree.library;

/**
 *
 * Created by leiko on 1/10/17.
 */
public class KevoreeParamException extends RuntimeException {

    public KevoreeParamException(String message) {
        super(message);
    }

    public KevoreeParamException(String instance, String param, String message) {
        super("Param \""+param+"\" from \""+instance+"\" " + message);
    }
}
