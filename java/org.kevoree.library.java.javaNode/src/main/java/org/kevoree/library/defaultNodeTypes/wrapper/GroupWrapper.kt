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

package org.kevoree.library.defaultNodeTypes.wrapper

import org.kevoree.library.defaultNodeTypes.reflect.FieldAnnotationResolver
import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import java.lang.reflect.InvocationTargetException
import org.kevoree.api.BootstrapService
import org.kevoree.ContainerRoot
import org.kevoree.log.Log
import org.kevoree.Group

public class GroupWrapper(val modelElement: Group, override val targetObj: Any, val nodeName: String, override var tg: ThreadGroup, override val bs: BootstrapService) : KInstanceWrapper {
    override var kcl: ClassLoader? = null

    override var isStarted: Boolean = false
    override val resolver = MethodAnnotationResolver(targetObj.javaClass);
    private val fieldResolver = FieldAnnotationResolver(targetObj.javaClass);

    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        if (!isStarted) {
            try {
                //target.getModelService()!!.registerModelListener(target)
                val met = resolver.resolve(javaClass<org.kevoree.annotation.Start>())
                met?.invoke(targetObj)
                isStarted = true
                return true
            } catch(e: InvocationTargetException) {
                Log.error("Kevoree Group Instance Start Error !", e.getCause())
                return false
            } catch(e: Exception) {
                Log.error("Kevoree Group Instance Start Error !", e)
                return false
            }
        } else {
            return false
        }
    }

    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        if (isStarted) {
            try {
                // target.getModelService()!!.unregisterModelListener(target)
                val met = resolver.resolve(javaClass<org.kevoree.annotation.Stop>())
                met?.invoke(targetObj)
                isStarted = false
                return true
            } catch(e: InvocationTargetException) {
                Log.error("Kevoree Group Instance Stop Error !", e.getCause())
                return false
            } catch (e: Exception) {
                Log.error("Kevoree Group Instance Stop Error !", e)
                return false
            }
        } else {
            return true
        }
    }

}