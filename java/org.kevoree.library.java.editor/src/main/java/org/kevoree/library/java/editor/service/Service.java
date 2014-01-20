package org.kevoree.library.java.editor.service;

import com.google.gson.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 20/01/14
 * Time: 11:58
 */
public interface Service {
    
    void process(ServiceCallback cb);
    
    public interface ServiceCallback {
        void onSuccess(JsonObject jsonRes);
        void onError(Exception e);
    }
}
