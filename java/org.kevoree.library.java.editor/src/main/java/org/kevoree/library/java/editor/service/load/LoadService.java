package org.kevoree.library.java.editor.service.load;

import com.google.gson.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 20/01/14
 * Time: 11:58
 */
public interface LoadService {
    
    JsonObject process() throws Exception;
}
