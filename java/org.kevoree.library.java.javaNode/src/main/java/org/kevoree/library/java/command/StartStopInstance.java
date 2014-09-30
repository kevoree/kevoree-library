package org.kevoree.library.java.command;

import org.kevoree.ContainerRoot;
import org.kevoree.Instance;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.library.java.wrapper.KInstanceWrapper;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.KMFContainer;


/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class StartStopInstance implements PrimitiveCommand, Runnable {

    private Instance c;
    private String nodeName;
    private boolean start;
    private ModelRegistry registry;
    private BootstrapService bs;
    private Thread t = null;
    private boolean resultAsync = false;
    private ContainerRoot root = null;
    private KInstanceWrapper iact = null;

    public StartStopInstance(Instance c, String nodeName, boolean start, ModelRegistry registry, BootstrapService bs) {
        this.c = c;
        this.nodeName = nodeName;
        this.start = start;
        this.registry = registry;
        this.bs = bs;
    }

    public void run() {
        try {
            // FIXME when a deployUnit will have multiple DeployUnit, we need to find the right one...
            Thread.currentThread().setContextClassLoader(iact.getKcl());
            ModelRegistry.current.set(registry);
            if (start) {
                Thread.currentThread().setName("KevoreeStartInstance" + c.getName());
                resultAsync = iact.kInstanceStart(root);
            } else {
                Thread.currentThread().setName("KevoreeStopInstance" + c.getName());
                boolean res = iact.kInstanceStop(root);
                if (!res) {
                    Log.error("Error while Stopping Wrapper ");
                }
                Thread.currentThread().setContextClassLoader(null);
                resultAsync = res;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void undo() {
        new StartStopInstance(c, nodeName, !start, registry, bs).execute();
    }

    public boolean execute() {

        if (start) {
            Log.info("Starting {}", c.path());
        } else {
            Log.info("Stopping {}", c.path());
        }

        //Look thread group

        KMFContainer r = c;
        if (r != null) {
            while (r.eContainer() != null) {
                r = r.eContainer();
            }
        }

        root = (ContainerRoot) r;
        Object ref = registry.lookup(c);
        if (ref != null && ref instanceof KInstanceWrapper) {
            iact = (KInstanceWrapper) ref;
            t = new Thread(iact.getTg(), this);
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!start) {
                //kill subthread
                Thread[] subThreads = new Thread[iact.getTg().activeCount()];
                iact.getTg().enumerate(subThreads);
                for (Thread subT : subThreads) {
                    try {
                        if (subT.isAlive()) {
                            subT.interrupt();
                        }
                    } catch (Throwable t) {
                        //ignore
                    }
                }
            }
            //call sub
            return resultAsync;
        } else {
            Log.error("issue while searching TG to start (or stop) thread");
            return false;
        }
    }

    public String toString() {
        String s = "StartStopInstance " + c.getName();
        if (start) {
            s += " start";
        } else {
            s += " stop";
        }
        return s;
    }

}
