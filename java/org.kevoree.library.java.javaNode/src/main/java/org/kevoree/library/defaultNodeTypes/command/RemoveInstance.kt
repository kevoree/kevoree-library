package org.kevoree.library.defaultNodeTypes.command

import org.kevoree.*
import org.kevoree.api.PrimitiveCommand
import org.kevoree.framework.kaspects.TypeDefinitionAspect
import org.kevoree.api.BootstrapService

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
class RemoveInstance(val c: Instance, val nodeName: String, val registry: MutableMap<String, Any>, val bs: BootstrapService) : PrimitiveCommand {

    private val typeDefinitionAspect = TypeDefinitionAspect()

    override fun undo() {
        try {
            AddInstance(c, nodeName, registry, bs).execute()
            UpdateDictionary(c, nodeName, registry).execute()
        } catch(e: Exception) {
            //
        }
    }

    override fun execute(): Boolean {
        try {
            registry.remove(c.path()!!)
            return true
        } catch(e: Exception){
            return false
        }
    }

    fun toString(): String {
        return "RemoveInstance ${c.name}"
    }

}
