package org.kevoree.library.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Protocol {
    public static final String REGISTER = "register";
    public static final int REGISTER_TYPE = 0;

    public static final String PUSH = "push";
    public static final int PUSH_TYPE = 1;

    public static final String PULL = "pull";
    public static final int PULL_TYPE = 2;

    public static final String KEVS = "kevs";
    public static final int KEVS_TYPE = 3;

    public static final String RESULT = "result";
    public static final int RESULT_TYPE = 4;

    public static final String STATUS = "status";
    public static final int STATUS_TYPE = 5;

    public static final String DIFF = "diff";
    public static final String SEP = "/";

    private final static Pattern patternPush = Pattern.compile("^push/(([^/{]+)/)?(.*)$",
            Pattern.DOTALL | Pattern.MULTILINE);
    private final static Pattern patternKevS = Pattern.compile("^kevs/(.*)$", Pattern.DOTALL | Pattern.MULTILINE);
    private final static Pattern patternResult = Pattern.compile("^result/([^/]+)/([^/]+)/(true|false)$");
    private final static Pattern patterStatus = Pattern.compile("^status/(.*)$");

    public interface Message {
        public int getType();

        public String toRaw();
    }

    public static class StatusMessage implements Message {

        private final String uid;

        public StatusMessage(final String uid) {
            this.uid = uid;
        }

        @Override
        public int getType() {
            return STATUS_TYPE;
        }

        @Override
        public String toRaw() {
            return "status/" + getUid();
        }

        public String getUid() {
            return uid;
        }

    }

    public static class ResultMessage implements Message {

        private final String node;
        private final String uid;
        private final Boolean result;

        public ResultMessage(String node, String uid, Boolean result) {
            this.node = node;
            this.uid = uid;
            this.result = result;
        }

        @Override
        public int getType() {
            return RESULT_TYPE;
        }

        @Override
        public String toRaw() {
            return "result/" + node + "/" + uid + "/" + result;
        }

        public String getNode() {
            return node;
        }

        public String getUid() {
            return uid;
        }

        public Boolean getResult() {
            return result;
        }

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

        private String uid;
        private final String model;

        public PushMessage(String model, String uid) {
            this.model = model;
            this.uid = uid;
        }

        public String getModel() {
            return model;
        }

        public String getUid() {
            return uid;
        }

        @Override
        public int getType() {
            return PUSH_TYPE;
        }

        @Override
        public String toRaw() {
            if (uid != null) {
                return PUSH + SEP + uid + SEP + model;
            } else {
                return PUSH + SEP + model;
            }
        }

        public void setUid(String uid) {
            this.uid = uid;

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
        if (msg.startsWith(REGISTER)) {
            return parseRegister(msg);
        }

        if (msg.startsWith(PULL)) {
            return new PullMessage();
        }

        Matcher matcher = patternPush.matcher(msg);
        if (matcher.matches()) {
            return parsePush(matcher);
        }

        matcher = patternKevS.matcher(msg);
        if (matcher.matches()) {
            return parseKevSPush(matcher);
        }

        matcher = patternResult.matcher(msg);
        if (matcher.matches()) {
            return parseResult(matcher);
        }

        matcher = patterStatus.matcher(msg);
        if (matcher.matches()) {
            return new StatusMessage(matcher.group(1));
        }

        return null;
    }

    private static Message parseResult(final Matcher resultMatcher) {
        final Message ret;
        final String node = resultMatcher.group(1);
        final String uid = resultMatcher.group(2);
        final boolean result = "true".equalsIgnoreCase(resultMatcher.group(3));
        ret = new ResultMessage(node, uid, result);
        return ret;
    }

    private static Message parsePush(final Matcher pushMatcher) {
        final String uid = pushMatcher.group(2);
        final String model = pushMatcher.group(3);
        return new PushMessage(model, uid);
    }

    private static Message parseKevSPush(final Matcher matcher) {
        final String script = matcher.group(1);
        return new PushKevSMessage(script);
    }

    private static Message parseRegister(final String msg) {
        final Message ret;
        final String payload = msg.substring(REGISTER.length() + SEP.length());
        int i = 0;
        char ch = payload.charAt(i);
        StringBuilder buffer = new StringBuilder();
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
        ret = new Protocol.RegisterMessage(buffer.toString(), model);
        return ret;
    }
}
