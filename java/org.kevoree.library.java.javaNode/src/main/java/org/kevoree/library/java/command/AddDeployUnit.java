package org.kevoree.library.java.command;



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

import org.kevoree.DeployUnit;
import org.kevoree.api.BootstrapService;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.api.helper.KModelHelper;
import org.kevoree.kcl.api.FlexyClassLoader;
import org.kevoree.log.Log;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 26/01/12
 * Time: 16:35
 */

public class AddDeployUnit implements PrimitiveCommand {

    private DeployUnit du;
    private org.kevoree.api.BootstrapService bs;

    public AddDeployUnit(DeployUnit du, BootstrapService bs) {
        this.du = du;
        this.bs = bs;
    }

    public void undo() {
        new RemoveDeployUnit(du, bs).execute();
    }

   public boolean execute() {
        try {
            FlexyClassLoader kclResolved = bs.get(du);
            if (kclResolved == null) {
                FlexyClassLoader new_kcl = bs.installDeployUnit(du);
                if (new_kcl != null) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        } catch(Exception e) {
            Log.debug("error ", e);
           return false;
        }
    }

    public String toString() {
        return "AddDeployUnit " + KModelHelper.fqnGroup(du) + "/" + du.getName() + "/" + du.getVersion() + "/" + du.getHashcode();
    }


}