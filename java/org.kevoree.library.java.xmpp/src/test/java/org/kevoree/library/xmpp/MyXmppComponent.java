package org.kevoree.library.xmpp;

import org.kevoree.library.xmpp.cmp.XmppComponent;
import java.util.Properties;

/**
 * @author ffouquet, gnain
 */
public class MyXmppComponent extends XmppComponent {

    /**
     * @param args the command line arguments
     */

    public static void main(String[] args) throws InterruptedException {
        // TODO code application logic here

        System.out.println("Beginning XMPP test");
        MyXmppComponent compo = new MyXmppComponent();

        compo.login = "entimid@gmail.com";
        compo.password = "entimidpass";

        compo.start();

        Properties msg = new Properties();
        msg.put("to", "gregory.nain@gmail.com");
        msg.put("content", "Yeepee");

        compo.send(msg);

        Thread.sleep(2 * 40 * 1000);

        compo.stop();
    }


}
