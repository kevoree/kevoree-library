package org.kevoree.library.defaultNodeTypes.command

import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.library.defaultNodeTypes.ModelRegistry
import org.kevoree.Instance
import org.kevoree.api.PrimitiveCommand
import org.kevoree.ContainerRoot
import org.kevoree.log.Log
import java.lang.reflect.InvocationTargetException
import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver

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

class UpdateDictionary(val c: Instance, val nodeName: String, val registry: ModelRegistry, val bs: org.kevoree.api.BootstrapService) : PrimitiveCommand {

    override fun execute(): Boolean {
        val reffound = registry.lookup(c)
        if(reffound != null){
            if(reffound is KInstanceWrapper){
                val iact = reffound as KInstanceWrapper
                val previousCL = Thread.currentThread().getContextClassLoader()
                Thread.currentThread().setContextClassLoader(iact.javaClass.getClassLoader())
                iact.kUpdateDictionary(c.typeDefinition!!.eContainer() as ContainerRoot, c)
                Thread.currentThread().setContextClassLoader(previousCL)
            } else {
                //case node type
                try {
                    bs.injectDictionary(c, reffound)
                    val resolver = MethodAnnotationResolver(reffound.javaClass)
                    val met = resolver.resolve(javaClass<org.kevoree.annotation.Update>())
                    met?.invoke(reffound)
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
        Log.error("hum, hum this is ambarrasing .....")
        /*
        val mapFound = registry.get(c.path()!!)
        val tempHash = HashMap<String, Any>()
        if (lastDictioanry != null) {
            tempHash.putAll(lastDictioanry!!);
        }
        if(mapFound != null && mapFound is KInstanceWrapper){
            val iact = mapFound as KInstanceWrapper
            val previousCL = Thread.currentThread().getContextClassLoader()
            Thread.currentThread().setContextClassLoader(iact.javaClass.getClassLoader())
            lastDictioanry = iact.kUpdateDictionary(tempHash, c.typeDefinition!!.eContainer() as ContainerRoot)
            Thread.currentThread().setContextClassLoader(previousCL)
        } */
    }

    fun toString(): String {
        return "UpdateDictionary ${c.name}"
    }

}
