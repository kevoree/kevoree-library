package org.kevoree.library.defaultNodeTypes.command

import org.kevoree.Instance
import org.kevoree.api.PrimitiveCommand
import org.kevoree.ComponentInstance
import org.kevoree.Group
import org.kevoree.Channel
import org.kevoree.log.Log
import org.kevoree.library.defaultNodeTypes.wrapper.ComponentWrapper
import org.kevoree.library.defaultNodeTypes.wrapper.GroupWrapper
import org.kevoree.library.defaultNodeTypes.wrapper.ChannelWrapper
import org.kevoree.ContainerNode
import org.kevoree.library.defaultNodeTypes.wrapper.NodeWrapper
import org.kevoree.api.BootstrapService
import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper

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

class AddInstance(val c: Instance, val nodeName: String, val registry: MutableMap<String, Any>, val bs: BootstrapService) : PrimitiveCommand, Runnable {

    var nodeTypeName: String? = null
    var tg: ThreadGroup? = null

    var resultSub = false

    override fun execute(): Boolean {
        var subThread: Thread? = null
        try {
            tg = ThreadGroup("kev/" + c.path()!!)
            subThread = Thread(tg, this)
            subThread!!.start()
            subThread!!.join()
            return resultSub
        } catch(e: Throwable) {
            if(subThread != null){
                try {
                    subThread!!.stop() //kill sub thread
                } catch(t: Throwable){
                    //ignore killing thread
                }
            }
            val message = "Could not add the instance " + c.name!! + ":" + c.typeDefinition!!.name!!
            Log.error(message, e)
            return false
        }
    }

    override fun undo() {
        RemoveInstance(c, nodeName, registry, bs).execute()
    }

    public override fun run() {
        try {
            val newBeanInstance = bs.createInstance(c)
            var newBeanKInstanceWrapper: KInstanceWrapper? = null
            if(c is ComponentInstance){
                newBeanKInstanceWrapper = ComponentWrapper(newBeanInstance!!, nodeName, c.name!!, tg!!, bs)
                (newBeanKInstanceWrapper as ComponentWrapper).initPorts(c, newBeanInstance)
            }
            if(c is Group){
                newBeanKInstanceWrapper = GroupWrapper(newBeanInstance as Any, nodeName, c.name!!, tg!!, bs)
            }
            if(c is Channel){
                newBeanKInstanceWrapper = ChannelWrapper(newBeanInstance as Any, nodeName, c.name!!, tg!!, bs)
            }
            if(c is ContainerNode){
                newBeanKInstanceWrapper = NodeWrapper(c, c.path()!!, tg!!, bs)
            }

            registry.put(c.path()!!, newBeanKInstanceWrapper!!)

            val sub = UpdateDictionary(c, nodeName, registry)
            resultSub = sub.execute()
        } catch(e: Throwable){
            Log.error("Error while adding instance {}", e, c.name)
            resultSub = false
        }
    }


    public override fun toString(): String? {
        return "AddInstance " + c.name
    }
}