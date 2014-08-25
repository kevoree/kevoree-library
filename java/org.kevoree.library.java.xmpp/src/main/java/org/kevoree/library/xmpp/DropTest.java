package org.kevoree.library.xmpp;

import javax.script.*;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by duke on 6/3/14.
 */
public class DropTest {

    public static void main(String[] args) throws ScriptException, FileNotFoundException, NoSuchMethodException {

        ScriptEngineManager scriptEngineManager =
                new ScriptEngineManager();
        ScriptEngine nashorn =
                scriptEngineManager.getEngineByName("nashorn");


        Bindings bindings = nashorn.createBindings();
        BeanClass bb = new BeanClass();
        bb.setParam("fromJava");
        bindings.put("modelService",bb);
        nashorn.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        nashorn.eval(new FileReader("/Users/duke/Documents/dev/kevoreeTeam/kevoree-library/java/org.kevoree.library.java.xmpp/src/main/java/org/kevoree/library/xmpp/DropTest.js"));
        System.out.println("Just after Evel");
        Invocable invocable = (Invocable) nashorn;

        Object result = invocable.invokeFunction("start");
        System.out.println("Just after calling start");

        System.out.println(result);
         /*
        Object result = invocable.invokeFunction("start");
        System.out.println(result);
        System.out.println(result.getClass());
          */







    }

}
