package org.kevoree.library.java.command;

import java.util.Random;

import org.kevoree.api.BootstrapService;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.DeployUnit;
import org.kevoree.log.Log;
import org.kevoree.api.helper.KModelHelper;

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

public class RemoveDeployUnit implements PrimitiveCommand {

    private DeployUnit du;
    private org.kevoree.api.BootstrapService bootstrap;

    public RemoveDeployUnit(DeployUnit du, BootstrapService bootstrap) {
        this.du = du;
        this.bootstrap = bootstrap;
    }

    //var random = Random();

    public void undo() {
        new AddDeployUnit(du, bootstrap).execute();
    }

    //LET THE UNINSTALL
    public boolean execute() {
        try {
            bootstrap.removeDeployUnit(du);
            //TODO cleanup links
            return true;

        }catch (Exception e) {
            Log.debug("error ", e);
            return false;
        }
    }

   public String toString() {
        return "RemoveDeployUnit " + KModelHelper.fqnGroup(du) + "/" + du.getName() + "/" + du.getVersion() + "/" + du.getHashcode();
    }
}