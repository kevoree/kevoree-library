package org.kevoree.library.java.command;

import org.kevoree.library.java.ModelRegistry;
import org.kevoree.Instance;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.library.java.wrapper.WrapperFactory;
import org.kevoree.library.java.wrapper.KInstanceWrapper;
import org.kevoree.api.ModelService;
import org.kevoree.log.Log;

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
public class RemoveInstance implements PrimitiveCommand {

    private WrapperFactory wrapperFactory;
    private Instance c;
    private String nodeName;
    private ModelRegistry registry;
    private BootstrapService bs;
    private ModelService modelService;

    public RemoveInstance(WrapperFactory wrapperFactory, Instance c, String nodeName, ModelRegistry registry, BootstrapService bs, ModelService modelService) {
        this.wrapperFactory = wrapperFactory;
        this.c = c;
        this.nodeName = nodeName;
        this.registry = registry;
        this.bs = bs;
        this.modelService = modelService;
    }

    public void undo() {
        try {
            KInstanceWrapper previouslyCreatedWrapper = null;
            if (registry != null) {
                previouslyCreatedWrapper = (KInstanceWrapper) registry.lookup(c);
            }

            if(previouslyCreatedWrapper != null) {
                Thread.currentThread().setContextClassLoader(previouslyCreatedWrapper.getKcl());
            } else {
                Thread.currentThread().setContextClassLoader(null);
            }
            Thread.currentThread().setName("KevoreeRemoveInstance" + c.getName());

            if (previouslyCreatedWrapper == null) {
                new AddInstance(wrapperFactory, c, nodeName, registry, bs, modelService).execute();
                Object newCreatedWrapper = registry.lookup(c);
                if (newCreatedWrapper instanceof KInstanceWrapper) {
                    bs.injectDictionary(c, ((KInstanceWrapper) newCreatedWrapper).getTargetObj(), false);
                }
            }

            Thread.currentThread().setContextClassLoader(null);

        } catch(Exception e) {
            Log.error("Error during rollback", e);
        }
    }

    public boolean execute() {
        try {
            Object previousWrapper = registry.lookup(c);
            if (previousWrapper instanceof KInstanceWrapper) {
                ((KInstanceWrapper)previousWrapper).destroy();
            }
            registry.drop(c);
            Log.info("Remove instance {}", c.path());
            return true;
        } catch(Exception e) {
            return false;
        }
    }

   public String toString() {
        return "RemoveInstance " + c.getName();
    }

}
