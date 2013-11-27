package org.kevoree.library.defaultNodeTypes.command

import org.kevoree.Instance
import org.kevoree.api.PrimitiveCommand
import java.util.HashMap

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 13/03/12
 * Time: 15:29
 */

class SelfDictionaryUpdate(val c: Instance, val registry: MutableMap<String, Any>) : PrimitiveCommand {

    private var lastDictioanry: HashMap<String, Any>? = null

    override fun execute(): Boolean {
        //BUILD MAP
        //SET DEFAULT VAL
        val dictionary: HashMap<String, Any> = HashMap<String, Any>()
        if (c.typeDefinition!!.dictionaryType != null) {
            for(dv in c.typeDefinition!!.dictionaryType!!.defaultValues) {
                dictionary.put(dv.attribute!!.name!!, dv.value!!)
            }
        }
        //SET DIC VAL
        if (c.dictionary != null) {
            for(v in c.dictionary!!.values) {
                dictionary.put(v.attribute!!.name!!, v.value!!)
            }
        }
        //SAVE DICTIONARY
       // lastDictioanry = node.getDictionary()
       // node.setDictionary(dictionary)
       // node.updateNode() // TODO REFLEXIVE CALL
        return true
    }

    override fun undo() {
        if (lastDictioanry != null) {
           // node.setDictionary(lastDictioanry)
           // node.updateNode() // TODO REFLEXIVE CALL
        }
    }

    fun toString(): String {
        return "SelfDictionaryUpdate " + c.name!!
    }

}
