package org.kevoree.library.feedback;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class ResultTaskService {
    public final ConcurrentHashMap<String, ResultTask> deploymentResultsMap = new ConcurrentHashMap<>();
    private final long delay;

    public enum Status {
        RUNNING, SUCCESS, FAIL, NOT_FOUND
    };

    private Map<String, Status> resultMap = new HashMap<>();

    public ResultTaskService(long delay) {
        this.delay = delay;
    }

    public void cleanup() {
        for (final Entry<String, ResultTask> entry : deploymentResultsMap.entrySet()) {
            cleanup(entry.getValue());
        }
    }

    public void cleanup(String uid) {
        cleanup(this.deploymentResultsMap.get(uid));
    }

    private void cleanup(final ResultTask value) {
        if (value != null) {
            final Timer timer = value.getTimer();
            timer.cancel();
            timer.purge();
        }
    }

    public void initUid(final String uid, int expectedNumberOfNodes) {
        if (!deploymentResultsMap.containsKey(uid)) {
            final ResultTask value = new ResultTask();
            value.setExpectedNumberOfNodes(expectedNumberOfNodes);
            deploymentResultsMap.put(uid, value);
        }
    }

    public void startTimer(final String node, final String uid, final Boolean result) {
        final ResultTask resultTask = deploymentResultsMap.get(uid);

        setStatus(uid, Status.RUNNING);

        Timer timer = new Timer();
        resultTask.setTimer(timer);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                if (deploymentResultsMap.containsKey(uid)) {
                    setStatus(uid, Status.FAIL);
                    cleanup(uid);
                }
            }

        }, delay);

        final ConcurrentHashMap<String, Boolean> nodesMap = resultTask.getNodesMap();
        nodesMap.put(node, result);
    }

    private void setStatus(final String uid, final Status status) {
        this.resultMap.put(uid, status);
    }

    public Status getStatus(final String uid) {
        Status ret;
        if (resultMap.containsKey(uid)) {
            ret = resultMap.get(uid);
        } else {
            ret = Status.NOT_FOUND;
        }
        return ret;
    }

    public void checkStatus() {
        /**
         * 1 - check if one of the results is negative. Save it as an error. 2 -
         * check if the number of results matches the number of nodes attached
         * to the group. the operation is a success
         * 
         * Cleanup : - Stopping the timeout Timer - cleaning the key in the map
         */
        for (final Entry<String, ResultTask> entry : deploymentResultsMap.entrySet()) {

            boolean hasError = false;
            final String uid = entry.getKey();
            for (final Entry<String, Boolean> nodeMapEntry : entry.getValue().getNodesMap().entrySet()) {
                if (!nodeMapEntry.getValue()) {
                    hasError = true;
                    break;
                }
            }

            if (hasError) {
                setStatus(uid, Status.FAIL);
                cleanup(uid);
            } else {
                if (entry.getValue().getExpectedNumberOfNodes() == entry.getValue().getNodesMap().size()) {
                    setStatus(uid, Status.SUCCESS);
                    cleanup(uid);
                }
            }
        }
    }
}
