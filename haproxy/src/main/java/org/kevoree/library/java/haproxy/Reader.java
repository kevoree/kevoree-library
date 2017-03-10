package org.kevoree.library.java.haproxy;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 04/12/2013
 * Time: 19:11
 */
public class Reader implements Runnable {

    private BufferedReader br;

    public Reader(InputStream i) {
        this.br = new BufferedReader(new InputStreamReader(i));
    }

    @Override
    public void run() {
        try {
            String value;
            while ((value = br.readLine()) != null) {
                System.out.println(value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
