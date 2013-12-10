package org.kevoree.library.defaultNodeTypes.wrapper.port;

import org.kevoree.api.Callback;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by duke on 10/12/2013.
 */
public class CallBackCaller {

    public static void call(Object result, Callback callback) {
        try {
            Type t = callback.getClass().getGenericInterfaces()[0];
            if (t instanceof ParameterizedType) {
                ((Class) ((ParameterizedType) t).getActualTypeArguments()[0]).cast(result);
            }
            callback.onSuccess(result);
        } catch (Exception e) {
            if (result != null) {
                callback.onError(new Exception("Bad Callback parameter " + result.getClass().getName()));
            } else {
                callback.onError(new Exception("Bad Callback parameter for null"));
            }
        }
    }

}
