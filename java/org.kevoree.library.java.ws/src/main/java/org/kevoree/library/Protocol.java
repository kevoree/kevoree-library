package org.kevoree.library;

/**
 * Created by duke on 6/2/14.
 */
public class Protocol {

    public static final String REGISTER = "register";
    public static final int REGISTER_TYPE = 0;

    public static final String PUSH = "push";
    public static final int PUSH_TYPE = 1;

    public static final String PULL = "pull";
    public static final int PULL_TYPE = 2;

    public static final String DIFF = "diff";
    public static final String SEP = "/";


    public interface Message {
        public int getType();

        public String toRaw();
    }

    public static class RegisterMessage implements Message {

        public RegisterMessage(String nodeName, String model) {
            this.nodeName = nodeName;
            this.model = model;
        }

        public String getNodeName() {
            return nodeName;
        }

        private String nodeName;

        public String getModel() {
            return model;
        }

        private String model;

        @Override
        public int getType() {
            return REGISTER_TYPE;
        }

        @Override
        public String toRaw() {
            StringBuilder buffer = new StringBuilder();
            buffer.append(REGISTER);
            buffer.append(SEP);
            buffer.append(nodeName);
            buffer.append(SEP);
            buffer.append(model);
            return buffer.toString();
        }
    }

    public static class PullMessage implements Message {

        @Override
        public int getType() {
            return PULL_TYPE;
        }

        @Override
        public String toRaw() {
            StringBuilder buffer = new StringBuilder();
            buffer.append(PULL);
            return buffer.toString();
        }
    }

    public static class PushMessage implements Message {

        public PushMessage(String model) {
            this.model = model;
        }

        public String getModel() {
            return model;
        }

        private String model;

        @Override
        public int getType() {
            return PUSH_TYPE;
        }

        @Override
        public String toRaw() {
            StringBuilder buffer = new StringBuilder();
            buffer.append(PUSH);
            buffer.append(SEP);
            buffer.append(model);
            return buffer.toString();
        }
    }

    public static Message parse(String msg) {
        if (msg.startsWith(REGISTER)) {
            String payload = msg.substring(REGISTER.length() + SEP.length());
            int i = 0;
            char ch = payload.charAt(i);
            StringBuffer buffer = new StringBuffer();
            while (i < payload.length() && ch != "/".charAt(0)) {
                i++;
                buffer.append(ch);
                ch = payload.charAt(i);
            }
            if (ch != "/".charAt(0)) {
                buffer.append(ch);
            } else {
                i++;
            }
            String model = null;
            if (i < payload.length()) {
                model = payload.substring(i, payload.length());
            }
            return new Protocol.RegisterMessage(buffer.toString(), model);
        }
        if (msg.startsWith(PUSH)) {
            String model = msg.substring(PUSH.length() + SEP.length());
            return new PushMessage(model);
        }
        if (msg.startsWith(PULL)) {
            return new PullMessage();
        }
        /* retro compat */
        if (msg.startsWith("get")) {
            return new PullMessage();
        }
        return new PushMessage(msg);
    }
}
