package org.kevoree.library;

import org.kevoree.annotation.*;
import org.kevoree.api.Context;
import org.kevoree.api.Port;

import java.util.Random;

@ComponentType(version = 1, description = "A Kevoree component that sends a 'tick' message at user-defined intervals")
public class Ticker implements Runnable {

	@KevoreeInject
	private Context context;

	private Thread thread;
	private Random rand = new Random();

	@Param
	private long period = 3000l;

	@Output
	private Port tick;

	@Param
	private boolean random = false;

	@Start
	public void start() {
		thread = new Thread(this);
		thread.setName("kev_" + context.getPath());
		thread.start();
	}

	@Stop
	public void stop() {
		thread.interrupt();
	}

	@Override
	public void run() {
        while (true) {
            try {
                Thread.sleep(period);
                String value = System.currentTimeMillis() + "";
                if (random) {
                    value = rand.nextInt(100) + "";
                }
                tick.send(value);
            } catch (InterruptedException ignore) {
                break;
            }
        }
	}
}
