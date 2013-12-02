package org.kevoree.library.defaultNodeTypes.command

import org.kevoree.library.defaultNodeTypes.wrapper.ComponentWrapper
import org.kevoree.library.defaultNodeTypes.wrapper.ChannelWrapper
import org.kevoree.library.defaultNodeTypes.ModelRegistry
import org.kevoree.MBinding
import org.kevoree.api.PrimitiveCommand
import org.kevoree.ComponentInstance
import org.kevoree.log.Log

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


class RemoveBindingCommand(val c: MBinding, val nodeName: String, val registry: ModelRegistry) : PrimitiveCommand {

    override fun undo() {
        AddBindingCommand(c, nodeName, registry).execute()
    }

    override fun execute(): Boolean {
        if(c == null){
            return false
        }else{
            val kevoreeChannelFound = registry.lookup(c.hub!!)
            val kevoreeComponentFound = registry.lookup(c.port!!.eContainer() as ComponentInstance)
            if(kevoreeChannelFound != null && kevoreeComponentFound != null && kevoreeComponentFound is ComponentWrapper && kevoreeChannelFound is ChannelWrapper){
                val portName = c.port!!.portTypeRef!!.name
                val foundNeedPort = kevoreeComponentFound.requiredPorts.get(portName)
                val foundHostedPort = kevoreeComponentFound.providedPorts.get(portName)
                if(foundNeedPort == null && foundHostedPort == null){
                    Log.info("Port instance not found in component")
                    return false
                }
                if (foundNeedPort != null) {
                    foundNeedPort.delegate = null
                    return true
                }
                if(foundHostedPort != null){
                    var component = (c.port!!.eContainer() as ComponentInstance)
                    kevoreeChannelFound.context.portsBinded.remove("$component/$portName")
                    return true
                }
                return false
            } else {
                return false
            }
        }
    }

    fun toString(): String {
        return "RemoveBindingCommand " + c.hub!!.name + "<->" + (c.port!!.eContainer() as ComponentInstance).name
    }

}
