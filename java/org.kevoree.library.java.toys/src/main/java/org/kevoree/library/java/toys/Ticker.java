package org.kevoree.library.java.toys;

import org.kevoree.annotation.*;
import org.kevoree.api.Callback;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 02/12/2013
 * Time: 15:10
 */
@ComponentType
public class Ticker implements Runnable {

    @Param(defaultValue = "3000")
    Long period = 3000l;

    private Thread t = null;

    @Output
    org.kevoree.api.Port tick;

    @Start
    public void start() {
        t = new Thread(this);
        t.start();
    }

    @Stop
    public void stop() {
        t.stop();
    }

    @Override
    public void run() {
        try {
            while (true) {
                Thread.sleep(period);
                tick.call(System.currentTimeMillis(), new Callback() {
                    @Override
                    public void run(Object result) {
                        System.out.println(result);
                    }
                });
            }
        } catch (InterruptedException e) {

        }
    }
}
