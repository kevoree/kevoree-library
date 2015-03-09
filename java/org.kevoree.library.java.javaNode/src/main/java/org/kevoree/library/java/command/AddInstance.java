package org.kevoree.library.java.command;

import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.library.java.wrapper.KInstanceWrapper;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.Instance;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.log.Log;
import org.kevoree.library.java.wrapper.WrapperFactory;
import org.kevoree.api.ModelService;
import org.kevoree.ContainerNode;
import org.kevoree.library.java.KevoreeThreadGroup;

/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 26/01/12
 * Time: 17:53
 */


public class AddInstance implements PrimitiveCommand, Runnable {

    private WrapperFactory wrapperFactory;
    private Instance c;
    private String nodeName;
    private ModelRegistry registry;
    private BootstrapService bs;
    private ModelService modelService;

    private ThreadGroup tg = null;
    private boolean resultSub = false;

    public AddInstance(WrapperFactory wrapperFactory, Instance c, String nodeName, ModelRegistry registry, BootstrapService bs, ModelService modelService) {
        this.wrapperFactory = wrapperFactory;
        this.c = c;
        this.nodeName = nodeName;
        this.registry = registry;
        this.bs = bs;
        this.modelService = modelService;
    }

    public boolean execute() {
        Thread subThread = null;
        try {
            tg = new KevoreeThreadGroup("kev/" + c.path());
            subThread = new Thread(tg, this);
            subThread.start();
            subThread.join();
            return resultSub;
        } catch(Throwable e) {
            if (subThread != null) {
                try {
                    //subThread.stop(); //kill sub thread
                    subThread.interrupt();
                } catch(Throwable t) {
                    //ignore killing thread
                }
            }
            Log.error("Could not add the instance {}:{}",c.getName(), c.getTypeDefinition().getName(), e);
            return false;
        }
    }

    public void undo() {
        new RemoveInstance(wrapperFactory, c, nodeName, registry, bs, modelService).execute();
    }

    public void run() {
        try {
            FlexyClassLoader newKCL = ClassLoaderHelper.createInstanceClassLoader(c, nodeName, bs);
            Thread.currentThread().setContextClassLoader(newKCL);
            Thread.currentThread().setName("KevoreeAddInstance" + c.getName());
            KInstanceWrapper newBeanKInstanceWrapper;
            if (c instanceof ContainerNode) {
                newBeanKInstanceWrapper = wrapperFactory.wrap(c, this/* nodeInstance is useless because launched as external process */, tg, bs, modelService);
                newBeanKInstanceWrapper.setKcl(newKCL);
                registry.register(c, newBeanKInstanceWrapper);
            } else {
                Object newBeanInstance = bs.createInstance(c, newKCL);
                newBeanKInstanceWrapper = wrapperFactory.wrap(c, newBeanInstance, tg, bs, modelService);
                newBeanKInstanceWrapper.setKcl(newKCL);
                registry.register(c, newBeanKInstanceWrapper);
                bs.injectDictionary(c, newBeanInstance, true);
            }
            newBeanKInstanceWrapper.create();
            resultSub = true;
            Thread.currentThread().setContextClassLoader(null);
            Log.info("Add instance {}", c.path());
        } catch(Throwable e) {
            Log.error("Error while adding instance {}", e, c.getName());
            resultSub = false;
        }
    }


    public String toString() {
        return "AddInstance " + c.getName();
    }
}