package org.kevoree.library.java.command

import org.kevoree.library.java.wrapper.KInstanceWrapper
import org.kevoree.library.java.ModelRegistry
import org.kevoree.Instance
import org.kevoree.api.BootstrapService
import org.kevoree.api.PrimitiveCommand
import org.kevoree.log.Log
import org.kevoree.library.java.wrapper.WrapperFactory
import org.kevoree.api.ModelService
import org.kevoree.ContainerNode
import org.kevoree.library.java.KevoreeThreadGroup

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


class AddInstance(val wrapperFactory: WrapperFactory, val c: Instance, val nodeName: String, val registry: ModelRegistry, val bs: BootstrapService, val modelService: ModelService) : PrimitiveCommand, Runnable {

    var nodeTypeName: String? = null
    var tg: ThreadGroup? = null

    var resultSub = false

    override fun execute(): Boolean {
        var subThread: Thread? = null
        try {
            tg = KevoreeThreadGroup("kev/" + c.path())
            subThread = Thread(tg, this)
            subThread!!.start()
            subThread!!.join()
            return resultSub
        } catch(e: Throwable) {
            if (subThread != null) {
                try {
                    subThread!!.stop() //kill sub thread
                } catch(t: Throwable) {
                    //ignore killing thread
                }
            }
            val message = "Could not add the instance " + c.name!! + ":" + c.typeDefinition!!.name!!
            Log.error(message, e)
            return false
        }
    }

    override fun undo() {
        RemoveInstance(wrapperFactory, c, nodeName, registry, bs, modelService).execute()
    }

    public override fun run() {
        try {
            var newKCL = ClassLoaderHelper.createInstanceClassLoader(c, nodeName, bs)!!
            Thread.currentThread().setContextClassLoader(newKCL)
            Thread.currentThread().setName("KevoreeAddInstance" + c.name!!)
            var newBeanKInstanceWrapper: KInstanceWrapper
            if (c is ContainerNode) {
                newBeanKInstanceWrapper: KInstanceWrapper? = wrapperFactory.wrap(c, this/* nodeInstance is useless because launched as external process */, tg!!, bs, modelService)
                newBeanKInstanceWrapper.kcl = newKCL
                registry.register(c, newBeanKInstanceWrapper)
            } else {
                val newBeanInstance = bs.createInstance(c, newKCL)
                newBeanKInstanceWrapper: KInstanceWrapper? = wrapperFactory.wrap(c, newBeanInstance!!, tg!!, bs, modelService)
                newBeanKInstanceWrapper.kcl = newKCL
                registry.register(c, newBeanKInstanceWrapper)
                bs.injectDictionary(c, newBeanInstance, true)
            }
            newBeanKInstanceWrapper.create()
            resultSub = true
            Thread.currentThread().setContextClassLoader(null)
            Log.info("Add instance {}", c.path())
        } catch(e: Throwable) {
            Log.error("Error while adding instance {}", e, c.name)
            resultSub = false
        }
    }


    public override fun toString(): String {
        return "AddInstance " + c.name
    }
}