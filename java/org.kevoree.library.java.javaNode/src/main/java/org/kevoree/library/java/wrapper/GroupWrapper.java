package org.kevoree.library.java.wrapper;

import org.kevoree.ContainerRoot;
import org.kevoree.log.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by duke on 9/26/14.
 */
public class GroupWrapper extends KInstanceWrapper {

    @Override
    public boolean kInstanceStart(ContainerRoot model) {
        if (!getIsStarted()) {
            try {
                Method met = getResolver().resolve(org.kevoree.annotation.Start.class);
                if (met != null) {
                    met.invoke(getTargetObj());
                }
                setIsStarted(true);
                return true;
            } catch (InvocationTargetException e) {
                Log.error("Kevoree Group Instance Start Error !", e.getCause());
                return false;
            } catch (Exception e) {
                Log.error("Kevoree Group Instance Start Error !", e);
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean kInstanceStop(ContainerRoot model) {
        if (getIsStarted()) {
            try {
                Method met = getResolver().resolve(org.kevoree.annotation.Stop.class);
                if (met != null) {
                    met.invoke(getTargetObj());
                }
                setIsStarted(false);
                return true;
            } catch (InvocationTargetException e) {
                Log.error("Kevoree Group Instance Stop Error !", e.getCause());
                return false;
            } catch (Exception e) {
                Log.error("Kevoree Group Instance Stop Error !", e);
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public void create() {

    }

    @Override
    public void destroy() {

    }
}
