package org.kevoree.library.defaultNodeTypes.command

import org.kevoree.library.defaultNodeTypes.ModelRegistry
import org.kevoree.Instance
import org.kevoree.api.BootstrapService
import org.kevoree.api.PrimitiveCommand
import org.kevoree.library.defaultNodeTypes.wrapper.WrapperFactory
import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.api.ModelService
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
class RemoveInstance(val wrapperFactory: WrapperFactory, val c: Instance, val nodeName: String, val registry: ModelRegistry, val bs: BootstrapService,val modelService : ModelService) : PrimitiveCommand {

    override fun undo() {
        try {

            val kcl = registry.lookup(c.typeDefinition!!.deployUnit) as ClassLoader
            Thread.currentThread().setContextClassLoader(kcl)
            Thread.currentThread().setName("KevoreeRemoveInstance" + c.name!!)

            AddInstance(wrapperFactory, c, nodeName, registry, bs,modelService).execute()
            val newCreatedWrapper = registry.lookup(c)
            if(newCreatedWrapper is KInstanceWrapper){
                bs.injectDictionary(c,newCreatedWrapper.targetObj,false)
            }

            Thread.currentThread().setContextClassLoader(null)

        } catch(e: Exception) {
            Log.error("Error during rollback",e)
        }
    }

    override fun execute(): Boolean {
        try {
            val previousWrapper = registry.lookup(c)
            if(previousWrapper is KInstanceWrapper){
                previousWrapper.destroy()
            }
            registry.drop(c)
            return true
        } catch(e: Exception){
            return false
        }
    }

    override fun toString(): String {
        return "RemoveInstance ${c.name}"
    }

}
