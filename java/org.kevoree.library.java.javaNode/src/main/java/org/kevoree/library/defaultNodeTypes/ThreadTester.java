package org.kevoree.library.defaultNodeTypes;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 30/04/13
 * Time: 08:52
 */
public class ThreadTester implements Runnable {

    private static ThreadGroup parentGroup = null;

    public static void main(String[] args) throws InterruptedException {
        parentGroup = new ThreadGroup("TG1");
        Thread t = new Thread(parentGroup, new ThreadTester());
        t.start();
        Thread.sleep(1000);
        System.out.println(parentGroup.activeCount());
    }

    @Override
    public void run() {
        System.out.println("Thread run " + Thread.currentThread().getId() + "-" + Thread.currentThread().getName());
        System.out.println(" tg:"+Thread.currentThread().getThreadGroup().getName()+"-"+Thread.currentThread().getThreadGroup().getParent().getName());
        if(parentGroup.activeCount() < 4){
            ThreadGroup tg2 = new ThreadGroup("TG2");
            Thread t = new Thread(tg2,this);   //Explicit Creation of Sub ThreadGroup
            //Thread t = new Thread(this); //Lazy attach to parent ThreadGroup
            t.start();
        }
        if(parentGroup.activeCount() == 4){
            ExecutorService service = Executors.newFixedThreadPool(1);
            service.submit(this);
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
