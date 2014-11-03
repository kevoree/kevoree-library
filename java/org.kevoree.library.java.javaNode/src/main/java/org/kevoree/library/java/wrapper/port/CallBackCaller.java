package org.kevoree.library.java.wrapper.port;

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

    public static Object callMethod(Method method, Object target, Object[] values) throws InvocationTargetException, IllegalAccessException {
        Parameter[] params = method.getParameters();
        for (int i=0; i < params.length; i++) {
            String paramType = params[i].getType().getTypeName();
            if (values[i] instanceof Number) {
                Number numberedValue = (Number) values[i];
                if (paramType.equals("int") || paramType.equals("java.lang.Integer")) {
                    values[i] = numberedValue.intValue();
                } else if (paramType.equals("float") || paramType.equals("java.lang.Float")) {
                    values[i] = numberedValue.floatValue();
                } else if (paramType.equals("double") || paramType.equals("java.lang.Double")) {
                    values[i] = numberedValue.doubleValue();
                } else if (paramType.equals("short") || paramType.equals("java.lang.Short")) {
                    values[i] = numberedValue.shortValue();
                } else if (paramType.equals("long") || paramType.equals("java.lang.Long")) {
                    values[i] = numberedValue.longValue();
                }
            }
        }
        return method.invoke(target, values);
    }
}
