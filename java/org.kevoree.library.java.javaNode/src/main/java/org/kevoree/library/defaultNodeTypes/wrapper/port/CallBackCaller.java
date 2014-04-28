package org.kevoree.library.defaultNodeTypes.wrapper.port;

import org.kevoree.api.Callback;

import java.lang.reflect.*;

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
                callback.onError(new Exception("Bad Callback parameter " + result.getClass().getName(), e));
            } else {
                callback.onError(new Exception("Bad Callback parameter for null", e));
            }
        }
    }

    public static Object callMethod(Method method,Object target,Object params) throws InvocationTargetException, IllegalAccessException {
       return method.invoke(target,(Object[]) params);
    }

}
