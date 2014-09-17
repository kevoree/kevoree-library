package org.kevoree.library.client;

import org.kevoree.library.ExternalMessageInjection;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by duke on 8/26/14.
 */
public class KevoreeRestClient {

    private String url;

    public KevoreeRestClient(String url) {
        this.url = url;
        if (this.url.endsWith("/")) {
            this.url = this.url.substring(0, this.url.length() - 1);
        }
    }

    public String send2Channel(String channelName, String payload) throws Exception {
        return send(ExternalMessageInjection.channelPath + channelName, payload);
    }

    public String send2Port(String componentName, String portName, String payload) throws Exception {
        return send(ExternalMessageInjection.componentPath + componentName + "/" + portName, payload);
    }

    private String send(String path, String payload) throws Exception {
        URL obj = new URL(url + path);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "KevoreeClient");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setDoOutput(true);

        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(payload);
        wr.flush();
        wr.close();
        int responseCode = con.getResponseCode();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

}
