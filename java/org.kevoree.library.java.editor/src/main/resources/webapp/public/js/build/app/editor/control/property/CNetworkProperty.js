define(["abstraction/property/KNetworkProperty","presentation/property/UINetworkProperty","control/AController","util/Pooffs"],function(e,t,n,r){function i(n){e.prototype.constructor.call(this,n),this._ui=new t(this),this._isKeyReady=!0,this._isValueReady=!0}return r.extends(i,n),r.extends(i,e),i.prototype.setKey=function(t){e.prototype.setKey.call(this,t),this._isKeyReady=!0,this._ui.c2pKeyValueSaved()},i.prototype.setValue=function(t){e.prototype.setValue.call(this,t),this._isValueReady=!0,this._ui.c2pValueValueSaved()},i.prototype.p2cChangeKey=function(e){var t=this.getLink();t.hasNetworkProperty(this._id)?t.containsKey(e,this._id)?this._ui.c2pDisplayError(e):this.setKey(e):t.containsKey(e)?this._ui.c2pDisplayError(e):this.setKey(e)},i.prototype.p2cChangeValue=function(e){this.setValue(e)},i.prototype.p2cStartChangeKey=function(){this._isKeyReady=!1},i.prototype.p2cStartChangeValue=function(){this._isValueReady=!1},i});