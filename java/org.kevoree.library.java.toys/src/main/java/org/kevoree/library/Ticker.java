package org.kevoree.library;

import org.kevoree.annotation.*;
import org.kevoree.api.Callback;
import org.kevoree.log.Log;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 02/12/2013
 * Time: 15:10
 */
@ComponentType
public class Ticker implements Runnable {

    private boolean running;
    private Random rand = new Random();

    @Param(defaultValue = "3000")
    Long period = 3000l;

    @Output
    org.kevoree.api.Port tick;

    @Param(optional = true, defaultValue = "false")
    Boolean random;

    @Start
    public void start() {
        Thread t = new Thread(this);
        running = true;
        t.start();
    }

    @Stop
    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(period);
                String value = System.currentTimeMillis() + "";
                if (random) {
                    value = rand.nextInt(100) + "";
                }
                tick.call(value, new Callback() {
                    @Override
                    public void onSuccess(Object result) {
                        if (result != null) {
                            System.out.println("ticker return : " + result);
                        }
                    }

                    @Override
                    public void onError(Throwable exception) {
                        Log.warn(exception.getMessage());
                    }
                });
            } catch (InterruptedException e) { /* ignore */ }
        }
    }
}
