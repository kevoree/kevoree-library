package org.kevoree.library.java.command;

import org.kevoree.*;
import org.kevoree.api.BootstrapService;
import org.kevoree.library.java.wrapper.KInstanceWrapper;
import org.kevoree.library.java.ModelRegistry;
import org.kevoree.api.PrimitiveCommand;
import org.kevoree.log.Log;

import org.kevoree.api.ModelService;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.Value;
import org.kevoree.pmodeling.api.KMFContainer;

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

public class UpdateDictionary implements PrimitiveCommand {

    private Instance c;
    private Value dicValue;
    private String nodeName;
    private ModelRegistry registry;
    private org.kevoree.api.BootstrapService bs;
    private ModelService modelService;

    public UpdateDictionary(Instance c, Value dicValue, String nodeName, ModelRegistry registry, BootstrapService bs, ModelService modelService) {
        this.c = c;
        this.dicValue = dicValue;
        this.nodeName = nodeName;
        this.registry = registry;
        this.bs = bs;
        this.modelService = modelService;
    }

    public boolean execute() {

        //protection for default value injection
        ContainerRoot previousModel = modelService.getCurrentModel().getModel();
        KMFContainer previousValue = previousModel.findByPath(dicValue.path());
        if (previousValue == null) {
            Instance parentDictionary = (Instance) dicValue.eContainer().eContainer();
            if (parentDictionary != null) {
                KMFContainer previousInstance = previousModel.findByPath(c.path());
                if(previousInstance != null){
                    DictionaryType dt = parentDictionary.getTypeDefinition().getDictionaryType();
                    DictionaryAttribute dicAtt = dt.findAttributesByID(dicValue.getName());
                    if(dicAtt != null && dicAtt.getDefaultValue() != null && dicAtt.getDefaultValue().equals(dicValue.getValue())) {
                        Log.debug("Do not reinject default {}", dicValue.getValue());
                        return true;
                    }
                }
            }
        }

        Object reffound = registry.lookup(c);
        if (reffound != null) {
            if (reffound instanceof KInstanceWrapper) {
                ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(((KInstanceWrapper) reffound).getTargetObj().getClass().getClassLoader());
                bs.injectDictionaryValue(dicValue, ((KInstanceWrapper) reffound).getTargetObj());
                Thread.currentThread().setContextClassLoader(previousCL);
            } else {
                //case node type
                try {
                    ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(reffound.getClass().getClassLoader());
                    bs.injectDictionaryValue(dicValue, reffound);
                    Thread.currentThread().setContextClassLoader(previousCL);
                    return true;
                } catch(Exception e) {
                    Log.error("Kevoree NodeType Instance Update Error !", e);
                    return false;
                }
            }
            return true;
        } else {
            Log.error("Can't update dictionary of " + c.getName());
            return false;
        }
    }

    public void undo() {
        try {
            //try to found old value
            String valueToInject  = null;
            KMFContainer previousValue = modelService.getCurrentModel().getModel().findByPath(dicValue.path());
            if (previousValue != null && previousValue instanceof Value) {
                valueToInject = ((Value)previousValue).getValue();
            } else {
                Instance instance = (Instance)dicValue.eContainer().eContainer();
                DictionaryAttribute dicAtt = instance.getTypeDefinition().getDictionaryType().findAttributesByID(dicValue.getName());
                if (dicAtt.getDefaultValue() != null && !dicAtt.getDefaultValue().equals("")) {
                    valueToInject = dicAtt.getDefaultValue();
                }
            }
            if (valueToInject != null) {
                Value fakeDicoValue = new DefaultKevoreeFactory().createValue();
                fakeDicoValue.setValue(valueToInject);
                fakeDicoValue.setName(dicValue.getName());
                Object reffoundO = registry.lookup(c);
                if (reffoundO != null) {
                    if (reffoundO instanceof KInstanceWrapper) {
                        KInstanceWrapper reffound = (KInstanceWrapper)reffoundO;
                        ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(reffound.getTargetObj().getClass().getClassLoader());
                        bs.injectDictionaryValue(fakeDicoValue, reffound.getTargetObj());
                        Thread.currentThread().setContextClassLoader(previousCL);
                    } else {
                        ClassLoader previousCL = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(reffoundO.getClass().getClassLoader());
                        bs.injectDictionaryValue(fakeDicoValue, reffoundO);
                        Thread.currentThread().setContextClassLoader(previousCL);
                    }
                }
            }
        } catch (Throwable e) {
            Log.debug("Error during rollback ", e);
        }
    }

    public String toString() {
        return "UpdateDictionary " + c.getName();
    }

}
