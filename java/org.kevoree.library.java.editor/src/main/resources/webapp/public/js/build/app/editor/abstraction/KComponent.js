define(["abstraction/KEntity","util/Pooffs","require"],function(e,t,n){function i(t,i){e.prototype.constructor.call(this,t,i),this._parent=null,this._name="comp"+r++,this._inputs=[],this._outputs=[];var s=t.getModel().findTypeDefinitionsByID(this._type+"/"),o=s.getProvided(),u=s.getRequired(),a=n("factory/CFactory").getInstance();for(var f=0;f<o.size();f++)this._inputs.push(a.newInputPort(o.get(f).getName()));for(var f=0;f<u.size();f++)this._outputs.push(a.newOutputPort(u.get(f).getName()));for(var f=0;f<this._inputs.length;f++)this._inputs[f].setComponent(this);for(var f=0;f<this._outputs.length;f++)this._outputs[f].setComponent(this)}var r=0;return i.ENTITY_TYPE="ComponentType",t.extends(i,e),i.prototype.getEntityType=function(){return i.ENTITY_TYPE},i.prototype.getParent=function(){return this._parent},i.prototype.setParent=function(e){this._parent=e},i.prototype.remove=function(){e.prototype.remove.call(this),this._parent&&this._parent.removeChild(this)},i.prototype.hasChildren=function(){return!1},i.prototype.getChildren=function(){return[]},i.prototype.getPort=function(e){for(var t=0;t<this._inputs.length;t++)if(this._inputs[t].getName()==e)return this._inputs[t];for(var t=0;t<this._outputs.length;t++)if(this._outputs[t].getName()==e)return this._outputs[t];return null},i.prototype.getInputs=function(){return this._inputs},i.prototype.getOutputs=function(){return this._outputs},i.prototype.accept=function(e){e.visitComponent(this)},i});