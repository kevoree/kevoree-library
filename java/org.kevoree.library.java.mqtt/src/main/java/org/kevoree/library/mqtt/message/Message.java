package org.kevoree.library.mqtt.message;

/**
 *
 * Created by leiko on 1/21/16.
 */
public class Message {

    public String topic;
    public String message;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
