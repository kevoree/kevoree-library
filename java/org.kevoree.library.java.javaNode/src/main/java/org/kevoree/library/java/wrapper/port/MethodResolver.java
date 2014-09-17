package org.kevoree.library.java.wrapper.port;

import org.kevoree.annotation.Input;

import java.lang.reflect.Method;

/**
 * Created by duke on 14/01/2014.
 */
public class MethodResolver {

    public static Method resolve(String methodName,Class clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                if (method.getAnnotation(Input.class) != null) {
                    return method;
                }
            }
        }
        if(clazz.getSuperclass()!=null){
            return resolve(methodName,clazz.getSuperclass());
        }
        return null;
    }

}
