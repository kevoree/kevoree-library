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
package org.kevoree.library.defaultNodeTypes.wrapper;

import org.kevoree.ComponentInstance
import org.kevoree.ContainerRoot
import org.kevoree.library.defaultNodeTypes.reflect.MethodAnnotationResolver
import org.kevoree.library.defaultNodeTypes.reflect.FieldAnnotationResolver
import org.kevoree.log.Log
import java.lang.reflect.InvocationTargetException
import org.kevoree.api.BootstrapService
import org.kevoree.library.defaultNodeTypes.wrapper.port.RequiredPortImpl
import org.kevoree.library.defaultNodeTypes.wrapper.port.ProvidedPortImpl
import java.util.HashMap

public class ComponentWrapper(val modelElement: ComponentInstance, override val targetObj: Any, val nodeName: String, override var tg: ThreadGroup, override val bs: BootstrapService) : KInstanceWrapper {

    public val providedPorts: HashMap<String, ProvidedPortImpl> = HashMap<String, ProvidedPortImpl>()
    public val requiredPorts: HashMap<String, RequiredPortImpl> = HashMap<String, RequiredPortImpl>()
    override var isStarted: Boolean = false

    {
        /* Init Required and Provided Port */
        for(requiredPort in modelElement.required){
            var field = targetObj.javaClass.getDeclaredField(requiredPort.portTypeRef!!.name!!)
            if(!field.isAccessible()){
                field.setAccessible(true)
            }
            var portWrapper = RequiredPortImpl(requiredPort.path()!!)
            field.set(targetObj, portWrapper)
            requiredPorts.put(requiredPort.portTypeRef!!.name!!, portWrapper)
        }
        for(providedPort in modelElement.provided){
            var portWrapper = ProvidedPortImpl(targetObj, providedPort.portTypeRef!!.name!!, providedPort.path()!!,this)
            providedPorts.put(providedPort.portTypeRef!!.name!!, portWrapper)
        }
    }

    override val resolver = MethodAnnotationResolver(targetObj.javaClass);

    private val fieldResolver = FieldAnnotationResolver(targetObj.javaClass);

    private fun buildPortBean(bean: String, portName: String): String {
        val packName = bean.subSequence(0, bean.lastIndexOf("."))
        val clazzName = bean.subSequence(bean.lastIndexOf(".") + 1, bean.length())
        return packName.toString() + ".kevgen." + clazzName + "PORT" + portName
    }

    override fun kInstanceStart(tmodel: ContainerRoot): Boolean {
        if (!isStarted){
            try {
                val met = resolver.resolve(javaClass<org.kevoree.annotation.Start>())
                met?.invoke(targetObj)
                isStarted = true
                return true
            } catch(e: InvocationTargetException){
                Log.error("Kevoree Component Instance Start Error !", e.getCause())
                isStarted = true //WE PUT COMPONENT IN START STATE TO ALLOW ROLLBACK TO UNSET VARIABLE
                return false
            } catch(e: Exception) {
                Log.error("Kevoree Component Instance Start Error !", e)
                isStarted = true //WE PUT COMPONENT IN START STATE TO ALLOW ROLLBACK TO UNSET VARIABLE
                return false
            }
        } else {
            Log.error("Already started !")
            return false
        }
    }

    override fun kInstanceStop(tmodel: ContainerRoot): Boolean {
        if (isStarted){
            try {
                val met = resolver.resolve(javaClass<org.kevoree.annotation.Stop>())
                met?.invoke(targetObj)
                isStarted = false
                return true
            } catch(e: InvocationTargetException){
                Log.error("Kevoree Component Instance Stop Error !", e.getCause())
                return false

            } catch(e: Exception) {
                Log.error("Kevoree Component Instance Stop Error !", e)
                return false
            }
        } else {
            return true
        }
    }

}
