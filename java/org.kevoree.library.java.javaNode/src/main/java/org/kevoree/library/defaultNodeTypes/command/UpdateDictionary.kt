package org.kevoree.library.defaultNodeTypes.command

import org.kevoree.ContainerRoot
import org.kevoree.Instance
import org.kevoree.api.PrimitiveCommand
import org.kevoree.library.defaultNodeTypes.wrapper.KInstanceWrapper
import org.kevoree.log.Log

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

class UpdateDictionary(val c: Instance, val nodeName: String, val registry: MutableMap<String, Any>) : PrimitiveCommand {

    override fun execute(): Boolean {
        val reffound = registry.get(c.path()!!)
        if(reffound != null && reffound is KInstanceWrapper){
            val iact = reffound as KInstanceWrapper
            val previousCL = Thread.currentThread().getContextClassLoader()
            Thread.currentThread().setContextClassLoader(iact.javaClass.getClassLoader())
            iact.kUpdateDictionary(c.typeDefinition!!.eContainer() as ContainerRoot, c)
            Thread.currentThread().setContextClassLoader(previousCL)
        } else {
            return false
        }
        return true
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
