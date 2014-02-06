package org.kevoree.library.java.toys;

import org.kevoree.annotation.*;
import org.kevoree.api.Callback;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 02/12/2013
 * Time: 15:10
 */
@ComponentType
@Library(name = "Java :: Toys")
public class TwoValueTicker implements Runnable {

    @Param(defaultValue = "3000")
    Long period = 3000l;

    private Thread t = null;

    @Output
    org.kevoree.api.Port tick;

    @Param(optional = true, defaultValue = "false")
    Boolean random;

    private Random rand = new Random();

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
                String value = System.currentTimeMillis() + "";
                if (random) {
                    value = rand.nextInt(100) + "";
                }
                String[] values = {value,period+""};
                tick.call(values,new Callback() {
                    @Override
                    public void onSuccess(Object result) {
                        if (result != null) {
                            System.out.println("ticker return : " + result);
                        }
                    }
                    @Override
                    public void onError(Throwable exception) {
                        exception.printStackTrace();
                    }
                });
            }
        } catch (InterruptedException e) {
        }
    }
}
