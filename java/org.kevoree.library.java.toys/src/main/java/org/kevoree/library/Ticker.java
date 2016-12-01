package org.kevoree.library;

import org.kevoree.annotation.*;
import org.kevoree.api.Callback;
import org.kevoree.api.CallbackResult;
import org.kevoree.api.Context;
import org.kevoree.api.Port;
import org.kevoree.log.Log;

import java.util.Random;

@ComponentType(version = 1, description = "A Kevoree component that sends a 'tick' message at user-defined intervals")
public class Ticker implements Runnable {

	@KevoreeInject
	private Context context;

	private boolean running;
	private Random rand = new Random();

	@Param(defaultValue = "3000", optional = true)
	private long period = 3000l;

	@Output
	private Port tick;

	@Param(defaultValue = "false", optional = true)
	private boolean random = false;

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
				tick.send(value, new Callback() {
					@Override
					public void onSuccess(CallbackResult result) {
						if (result != null) {
							Log.debug("ticker return : " + result.getPayload());
						}
					}

					@Override
					public void onError(Throwable exception) {
						Log.warn(exception.getMessage());
					}
				});
			} catch (InterruptedException e) {
				/* ignore */ }
		}
	}
}
