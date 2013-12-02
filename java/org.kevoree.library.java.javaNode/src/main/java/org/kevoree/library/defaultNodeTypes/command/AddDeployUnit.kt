package org.kevoree.library.defaultNodeTypes.command

import org.kevoree.library.defaultNodeTypes.ModelRegistry
import org.kevoree.DeployUnit
import org.kevoree.api.PrimitiveCommand
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
/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 26/01/12
 * Time: 16:35
 */

class AddDeployUnit(val du: DeployUnit, val bs: org.kevoree.api.BootstrapService, val registry: ModelRegistry) : PrimitiveCommand {

    override fun undo() {
        RemoveDeployUnit(du, bs, registry).execute()
    }

    override fun execute(): Boolean {
        try {
            var kclResolved = bs.get(du)
            if (kclResolved == null) {
                val new_kcl = bs.installDeployUnit(du)
                if (new_kcl != null) {
                    registry.register(du, new_kcl)
                    return true
                } else {
                    return false
                }
            } else {
                if(registry.lookup(du) == null){
                    registry.register(du, kclResolved)
                }
                return true
            }
        } catch(e: Exception) {
            Log.debug("error ", e); return false
        }
    }

    fun toString(): String {
        return "AddDeployUnit " + du.groupName + "/" + du.name + "/" + du.version + "/" + du.hashcode
    }


}