package org.kevoree.library.java.editor.service;

import com.google.gson.JsonObject;

public interface ServiceCallback {
        void onSuccess(JsonObject jsonRes);
        void onError(Exception e);
    }