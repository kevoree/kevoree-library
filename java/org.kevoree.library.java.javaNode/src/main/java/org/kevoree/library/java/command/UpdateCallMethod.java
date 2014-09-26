package org.kevoree.library.java.command;

import org.kevoree.api.BootstrapService;
import org.kevoree.library.java.wrapper.KInstanceWrapper;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.Instance;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.ContainerRoot;
import org.kevoree.log.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.kevoree.library.java.reflect.MethodAnnotationResolver;

/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class UpdateCallMethod implements PrimitiveCommand {

    private Instance c;
    private String nodeName;
    private ModelRegistry registry;
    private org.kevoree.api.BootstrapService bs;

    public UpdateCallMethod(Instance c, String nodeName, ModelRegistry registry, BootstrapService bs) {
        this.c = c;
        this.nodeName = nodeName;
        this.registry = registry;
        this.bs = bs;
    }

    public boolean execute() {
        Object reffound = registry.lookup(c);
        if (reffound != null) {
            if (reffound instanceof KInstanceWrapper) {
                if (((KInstanceWrapper)reffound).getIsStarted()) {
                    ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(((KInstanceWrapper) reffound).getTargetObj().getClass().getClassLoader());
                    MethodAnnotationResolver resolver = new MethodAnnotationResolver(((KInstanceWrapper) reffound).getTargetObj().getClass());
                    Method met = resolver.resolve(org.kevoree.annotation.Update.class);
                    if(met != null) {
                        try {
                            met.invoke(((KInstanceWrapper) reffound).getTargetObj());
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.error("Method not resolved org.kevoree.annotation.Update.class");
                    }
                    Thread.currentThread().setContextClassLoader(previousCL);
                }
            } else {
                //case node type
                try {
                    ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(reffound.getClass().getClassLoader());
                    MethodAnnotationResolver resolver = new MethodAnnotationResolver(reffound.getClass());
                    Method met = resolver.resolve(org.kevoree.annotation.Update.class);
                    if(met != null) {
                        met.invoke(reffound);
                    } else {
                        Log.error("Method not resolved org.kevoree.annotation.Update.class");
                    }
                    Thread.currentThread().setContextClassLoader(previousCL);
                    return true;
                } catch(InvocationTargetException e){
                    Log.error("Kevoree NodeType Instance Update Error !", e.getCause());
                    return false;
                } catch(Exception e) {
                    Log.error("Kevoree NodeType Instance Update Error !", e);
                    return false;
                }
            }
            return true;
        } else {
            Log.error("Can update dictionary of " + c.getName());
            return false;
        }
    }

    public void undo() {
        //Log.error("Rollback update dictionary not supported yet !!!")
    }

    public String toString() {
        return "UpdateCallMethod " + c.getName();
    }

}
