package org.kevoree.library.client;

/**
 * Created by duke on 8/26/14.
 */
public class KevoreeRestClientSample {

    public static void main(String[] args) throws Exception {
        KevoreeRestClient client = new KevoreeRestClient("http://localhost:8090/");
        System.out.println(client.send2Channel("hub0", "injectedFromExternalSource"));
        System.out.println(client.send2Port("print", "input", "injectedFromExternalSource"));
    }

}
