package org.kevoree.library.java.wrapper;

import org.kevoree.ContainerRoot;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.ModelService;
import org.kevoree.library.java.reflect.FieldAnnotationResolver;
import org.kevoree.library.java.reflect.MethodAnnotationResolver;
import org.kevoree.pmodeling.api.KMFContainer;

/**
 * Created by duke on 9/26/14.
 */
public abstract class KInstanceWrapper {

    private ThreadGroup tg;
    private Boolean isStarted;
    private BootstrapService bs;
    private ClassLoader kcl;
    private MethodAnnotationResolver resolver;
    private Object targetObj;

    public ModelService getModelService() {
        return modelService;
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }

    private ModelService modelService;

    public FieldAnnotationResolver getFieldResolver() {
        return fieldResolver;
    }

    public void setFieldResolver(FieldAnnotationResolver fieldResolver) {
        this.fieldResolver = fieldResolver;
    }

    private FieldAnnotationResolver fieldResolver;

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    private String nodeName;

    public KMFContainer getModelElement() {
        return modelElement;
    }

    public void setModelElement(KMFContainer modelElement) {
        this.modelElement = modelElement;
    }

    private KMFContainer modelElement;

    public Object getTargetObj() {
        return targetObj;
    }

    public void setTargetObj(Object targetObj) {
        this.targetObj = targetObj;
        setResolver(new MethodAnnotationResolver(targetObj.getClass()));
        setFieldResolver(new FieldAnnotationResolver(targetObj.getClass()));
    }

    public MethodAnnotationResolver getResolver() {
        return resolver;
    }

    public void setResolver(MethodAnnotationResolver resolver) {
        this.resolver = resolver;
    }

    public ClassLoader getKcl() {
        return kcl;
    }

    public void setKcl(ClassLoader kcl) {
        this.kcl = kcl;
    }

    public BootstrapService getBs() {
        return bs;
    }

    public void setBs(BootstrapService bs) {
        this.bs = bs;
    }

    public Boolean getIsStarted() {
        return isStarted;
    }

    public void setIsStarted(Boolean isStarted) {
        this.isStarted = isStarted;
    }

    public ThreadGroup getTg() {
        return tg;
    }

    public void setTg(ThreadGroup tg) {
        this.tg = tg;
    }

    public abstract boolean kInstanceStart(ContainerRoot model);

    public abstract boolean kInstanceStop(ContainerRoot model);

    public abstract void create();

    public abstract void destroy();

}
