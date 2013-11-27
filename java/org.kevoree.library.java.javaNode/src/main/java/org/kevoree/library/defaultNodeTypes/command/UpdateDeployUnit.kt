package org.kevoree.library.defaultNodeTypes.command

import org.kevoree.DeployUnit
import java.util.Random
import org.kevoree.kcl.KevoreeJarClassLoader
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

class UpdateDeployUnit(val du: DeployUnit, val bs: org.kevoree.api.BootstrapService, val registry: MutableMap<String, Any>) : EndAwareCommand {

    var lastKCL: KevoreeJarClassLoader? = null
    var random = Random()

    override fun undo() {
        if(lastKCL != null){
            bs.removeDeployUnit(du)
            bs.manualAttach(du, lastKCL!!)
        }
    }

    override fun execute(): Boolean {
        try {
            lastKCL = bs.get(du)
            bs.removeDeployUnit(du)
            bs.installDeployUnit(du)
            return true
        } catch (e: Exception) {
            Log.debug("error ", e);return false
        }
    }

    override fun doEnd() {
        lastKCL = null
    }

    fun toString(): String {
        return "UpdateDeployUnit " + du.groupName + "/" + du.name + "/" + du.version + "/" + du.hashcode
    }
}