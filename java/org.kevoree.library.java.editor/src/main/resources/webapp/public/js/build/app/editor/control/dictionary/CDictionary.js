define(["abstraction/dictionary/KDictionary","control/AController","presentation/dictionary/UIDictionary","util/Pooffs"],function(e,t,n,r){function i(t,r){r=r||!1,e.prototype.constructor.call(this,t,r),this._ui=new n(this)}return r.extends(i,t),r.extends(i,e),i.prototype.p2cSaveDictionary=function(e,t){for(var n=0;n<this._values.length;n++){var r=this._values[n].getAttribute().getName();if(this._values[n].getAttribute().getFragmentDependant()){var i=this._values[n].getTargetNode().getName();this._values[n].setValue(t[i][r])}else this._values[n].setValue(e[r])}this.getEntity().getEditor().updateModel(this)},i});