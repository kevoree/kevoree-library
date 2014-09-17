package org.kevoree.library.java.command

import java.util.Random
import org.kevoree.library.java.ModelRegistry
import org.kevoree.api.PrimitiveCommand
import org.kevoree.DeployUnit
import org.kevoree.log.Log
import org.kevoree.api.helper.KModelHelper

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

class RemoveDeployUnit(val du: DeployUnit, val bootstrap: org.kevoree.api.BootstrapService) : PrimitiveCommand {

    var random = Random()

    override fun undo() {
        AddDeployUnit(du, bootstrap).execute()
    }

    //LET THE UNINSTALL
    override fun execute(): Boolean {
        try {
            bootstrap.removeDeployUnit(du)
            //TODO cleanup links
            return true

        }catch (e: Exception) {
            Log.debug("error ", e)
            return false
        }
    }

    override fun toString(): String {
        return "RemoveDeployUnit " + KModelHelper.fqnGroup(du) + "/" + du.name + "/" + du.version + "/" + du.hashcode
    }
}