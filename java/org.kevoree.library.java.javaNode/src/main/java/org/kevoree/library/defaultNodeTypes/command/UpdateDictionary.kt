package org.kevoree.library.defaultNodeTypes.command

import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.library.defaultNodeTypes.ModelRegistry
import org.kevoree.Instance
import org.kevoree.api.PrimitiveCommand
import org.kevoree.ContainerRoot
import org.kevoree.log.Log
import java.lang.reflect.InvocationTargetException
import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import org.kevoree.DictionaryValue
import org.kevoree.api.ModelService
import org.kevoree.impl.DefaultKevoreeFactory

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

class UpdateDictionary(val c: Instance, val dicValue: DictionaryValue, val nodeName: String, val registry: ModelRegistry, val bs: org.kevoree.api.BootstrapService, val modelService : ModelService) : PrimitiveCommand {

    override fun execute(): Boolean {
        val reffound = registry.lookup(c)
        if (reffound != null) {
            if (reffound is KInstanceWrapper) {
                val previousCL = Thread.currentThread().getContextClassLoader()
                Thread.currentThread().setContextClassLoader(reffound.targetObj.javaClass.getClassLoader())
                bs.injectDictionaryValue(dicValue, reffound.targetObj)
                Thread.currentThread().setContextClassLoader(previousCL)
            } else {
                //case node type
                try {
                    val previousCL = Thread.currentThread().getContextClassLoader()
                    Thread.currentThread().setContextClassLoader(reffound.javaClass.getClassLoader())
                    bs.injectDictionaryValue(dicValue, reffound)
                    Thread.currentThread().setContextClassLoader(previousCL)
                    return true
                } catch(e: InvocationTargetException){
                    Log.error("Kevoree NodeType Instance Update Error !", e.getCause())
                    return false
                } catch(e: Exception) {
                    Log.error("Kevoree NodeType Instance Update Error !", e)
                    return false
                }
            }
            return true
        } else {
            Log.error("Can update dictionary of " + c.name)
            return false
        }
    }

    override fun undo() {
        try {
            //try to found old value
            var valueToInject : String? = null
            val previousValue = modelService.getCurrentModel()?.getModel()?.findByPath(dicValue.path()!!)
            if(previousValue != null && previousValue is DictionaryValue){
                valueToInject = previousValue.value
            } else {
                val instance : Instance = dicValue.eContainer()?.eContainer() as Instance
                val dicAtt = instance.typeDefinition!!.dictionaryType!!.findAttributesByID(dicValue.name!!)!!
                if(dicAtt.defaultValue != null && dicAtt.defaultValue != ""){
                    valueToInject = dicAtt.defaultValue
                }
            }
            if(valueToInject!= null){
                val fakeDicoValue = DefaultKevoreeFactory().createDictionaryValue()
                fakeDicoValue.value = valueToInject
                fakeDicoValue.name = dicValue.name
                val reffound = registry.lookup(c)
                if (reffound != null) {
                    if (reffound is KInstanceWrapper) {
                        val previousCL = Thread.currentThread().getContextClassLoader()
                        Thread.currentThread().setContextClassLoader(reffound.targetObj.javaClass.getClassLoader())
                        bs.injectDictionaryValue(fakeDicoValue, reffound.targetObj)
                        Thread.currentThread().setContextClassLoader(previousCL)
                    } else {
                        val previousCL = Thread.currentThread().getContextClassLoader()
                        Thread.currentThread().setContextClassLoader(reffound.javaClass.getClassLoader())
                        bs.injectDictionaryValue(fakeDicoValue, reffound)
                        Thread.currentThread().setContextClassLoader(previousCL)
                    }
                }
            }
        } catch (e:Throwable){
           Log.debug("Error during rollback ",e)
        }
    }

    override fun toString(): String {
        return "UpdateDictionary ${c.name}"
    }

}
