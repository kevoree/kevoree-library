package org.kevoree.library.java.command;

import org.kevoree.library.java.wrapper.ComponentWrapper;
import org.kevoree.library.java.wrapper.ChannelWrapper;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.MBinding;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.ComponentInstance;
import org.kevoree.library.java.wrapper.port.ProvidedPortImpl;
import org.kevoree.library.java.wrapper.port.RequiredPortImpl;
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


public class RemoveBindingCommand implements PrimitiveCommand {

    private MBinding c;

    public RemoveBindingCommand(MBinding c, String nodeName, ModelRegistry registry) {
        this.c = c;
        this.nodeName = nodeName;
        this.registry = registry;
    }

    private  String nodeName;
    private ModelRegistry registry;

    public void undo() {
        new AddBindingCommand(c, nodeName, registry).execute();
    }

    public boolean execute() {
        if(c == null){
            return false;
        }else{
            Object kevoreeChannelFound = registry.lookup(c.getHub());
            Object kevoreeComponentFound = registry.lookup(c.getPort().eContainer());
            if(kevoreeChannelFound != null && kevoreeComponentFound != null && kevoreeComponentFound instanceof ComponentWrapper && kevoreeChannelFound instanceof ChannelWrapper){
                String portName = c.getPort().getPortTypeRef().getName();
                RequiredPortImpl foundNeedPort = ((ComponentWrapper) kevoreeComponentFound).getRequiredPorts().get(portName);
                ProvidedPortImpl foundHostedPort = ((ComponentWrapper) kevoreeComponentFound).getProvidedPorts().get(portName);
                if(foundNeedPort == null && foundHostedPort == null){
                    Log.info("Port instance not found in component");
                    return false;
                }
                if (foundNeedPort != null) {
                    foundNeedPort.getDelegate().remove(kevoreeChannelFound);
                    return true;
                }
                if(foundHostedPort != null){
                    //Seems useless
                    //ComponentInstance component = (ComponentInstance)c.getPort().eContainer();
                    ((ChannelWrapper) kevoreeChannelFound).getContext().getPortsBinded().remove(foundHostedPort.getPath());
                    return true;
                }
                return false;
            } else {
                return false;
            }
        }
    }

    public String toString() {
        String s = "RemoveBindingCommand ";
        if(c.getHub() != null) {
            s += c.getHub().getName();
        } else {
            s += " hub:null";
        }
        s += "<->";
        if(c.getPort() != null) {
            s += ((ComponentInstance)c.getPort().eContainer()).getName();
        } else {
            s += " port:null";
        }
        return  s ;
    }

}
