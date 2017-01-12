package org.kevoree.library.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Protocol {
    private static final String REGISTER = "register";
    public static final int REGISTER_TYPE = 0;

    private static final String PUSH = "push";
    public static final int PUSH_TYPE = 1;

    private static final String PULL = "pull";
    public static final int PULL_TYPE = 2;

    private static final String KEVS = "kevs";
    public static final int KEVS_TYPE = 3;

    private static final String SEP = "/";

    private final static Pattern patternPush = Pattern.compile("^push/(.*)$", Pattern.DOTALL | Pattern.MULTILINE);
    private final static Pattern patternKevS = Pattern.compile("^kevs/(.*)$", Pattern.DOTALL | Pattern.MULTILINE);
    private final static Pattern patternRegister = Pattern.compile("^register/([^/]+)/(.*)$",
            Pattern.DOTALL | Pattern.MULTILINE);

    public static String getTypeName(int type) {
        switch (type) {
            case PULL_TYPE:
                return PULL;
            case PUSH_TYPE:
                return PUSH;
            case KEVS_TYPE:
                return KEVS;
            case REGISTER_TYPE:
                return REGISTER;
            default:
                return null;
        }
    }

    public interface Message {
        int getType();
        String toRaw();
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
            return REGISTER + SEP + nodeName + SEP + model;
        }
    }

    public static class PullMessage implements Message {

        @Override
        public int getType() {
            return PULL_TYPE;
        }

        @Override
        public String toRaw() {
            return PULL;
        }
    }

    public static class PushMessage implements Message {

        private final String model;

        public PushMessage(String model) {
            this.model = model;
        }

        public String getModel() {
            return model;
        }

        @Override
        public int getType() {
            return PUSH_TYPE;
        }

        @Override
        public String toRaw() {
            return PUSH + SEP + model;
        }
    }

    public static class PushKevSMessage implements Message {

        private final String script;

        public PushKevSMessage(String script) {
            this.script = script;
        }

        public String getKevScript() {
            return script;
        }

        @Override
        public int getType() {
            return KEVS_TYPE;
        }

        @Override
        public String toRaw() {
            return KEVS + SEP + script;
        }
    }

    public static Message parse(final String msg) {
        Matcher matcher;

        if (msg.startsWith(PULL)) {
            return new PullMessage();
        }

        matcher = patternRegister.matcher(msg);
        if (matcher.matches()) {
            return parseRegister(matcher);
        }

        matcher = patternPush.matcher(msg);
        if (matcher.matches()) {
            return parsePush(matcher);
        }

        matcher = patternKevS.matcher(msg);
        if (matcher.matches()) {
            return parseKevSPush(matcher);
        }

        return null;
    }

    private static Message parsePush(final Matcher pushMatcher) {
        final String model = pushMatcher.group(1);
        return new PushMessage(model);
    }

    private static Message parseKevSPush(final Matcher matcher) {
        final String script = matcher.group(1);
        return new PushKevSMessage(script);
    }

    private static Message parseRegister(final Matcher matcher) {
        final String nodeName = matcher.group(1);
        final String model = matcher.group(2);
        return new RegisterMessage(nodeName, model);
    }
}
