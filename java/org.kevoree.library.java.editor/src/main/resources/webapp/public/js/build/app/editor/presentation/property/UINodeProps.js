define(["jquery","util/Pooffs","util/AlertPopupHelper","presentation/property/UIInstanceProps","templates/node-properties","bootstrap/multiselect"],function(e,t,n,r,i,s){function v(e){r.prototype.constructor.call(this,e)}function m(t){e("#"+f+' a[data-toggle="tab"]').off("shown"+o),e("#"+f+' a[data-toggle="tab"]').on("shown"+o,function(n){for(var r in t)t[r].getUI().setActive(t[r]._id==e(n.target).parent().attr(d))})}var o=".ui-node-props",u="node-push-action",a="node-pull-action",f="node-links-tabs",l="node-links-contents",c="initby-node",h="node-link-add",p="node-link-delete",d="data-node-link-id";return v.INIT_BY_NODES="initby-nodes",v.NODE_NETWORK_IP="node-network-ip",v.NODE_LINKS_PROP="node-links-properties",t.extends(v,r),v.prototype.getHTML=function(){function o(){var e=[],r=t.getNodes(),i=n._ctrl.getNodeNetworks();for(var s=0;s<r.size();s++){var o=!1;for(var u=0;u<i.length;u++)i[u].getInitBy().getName()==r.get(s).getName()&&(o=!0);e.push({selected:o,name:r.get(s).getName()})}return e}function u(){var e=[],t=n._ctrl.getLinks(),r=!1;for(var i in t)t[i].getUI().isActive()&&(r=!0);r||t[0].getUI().setActive(!0);for(var i in t)e.push({tabHTML:t[i].getUI().getTabHTML(),contentHTML:t[i].getUI().getContentHTML()});return e}function a(e){var n=t.getGroups(),r=[];for(var i=0;i<n.size();i++){var s=n.get(i).getSubNodes();for(var o=0;o<s.size();o++)s.get(o).getName()==e&&r.push(n.get(i).getName())}return r}var e=r.prototype.getHTML.call(this),t=this._ctrl.getEditor().getModel(),n=this,s={initBy:o(),nodeLinks:u(),groups:a(this._ctrl.getName())};return e+i(s)},v.prototype.onHTMLAppended=function(){r.prototype.onHTMLAppended.call(this);var t=this;this._ctrl.getLinks().length>1&&this.c2pEnableDeleteNodeLinkButton(),e("#"+v.INIT_BY_NODES).multiselect({includeSelectAllOption:!0,maxHeight:200,onChange:function(n,r){console.log("onChange",e(n).val()),e(n).hasClass("initby-node")&&(r?t._ctrl.p2cSelectedNodeNetwork(e(n).val()):t._ctrl.p2cUnselectedNodeNetwork(e(n).val()))}});var n=e("#"+u),i=e("#"+a);n.off(o),n.on("click"+o,function(){e("#node-push-pull-error").hide(),t._ctrl.p2cPushModel(e("#node-group-action").val())}),i.off(o),i.on("click"+o,function(){e("#node-push-pull-error").hide(),t._ctrl.p2cPullModel(e("#node-group-action").val())}),e("#"+h).off(o),e("#"+h).on("click"+o,function(){t._ctrl.p2cAddNodeLink()}),e("#"+p).off(o),e("#"+p).on("click"+o,function(){var n=e("#"+f+" li.active");t._ctrl.p2cDeleteNodeLink(parseInt(n.attr(d)))});var s=this._ctrl.getLinks();m(s);for(var l=0;l<s.length;l++)s[l].getUI().onHTMLAppended()},v.prototype.onSaveProperties=function(){r.prototype.onSaveProperties.call(this),this._ctrl.p2cSaveNetworkProperties()},v.prototype.getPropertiesValues=function(){var t=r.prototype.getPropertiesValues.call(this),n=[];return e("#"+v.INIT_BY_NODES+" option."+c+":selected").each(function(){n.push(e(this).val())}),t[v.INIT_BY_NODES]=n,t},v.prototype.c2pSelectNodeNetwork=function(t){e("#"+v.INIT_BY_NODES).multiselect("select",t)},v.prototype.c2pNodeLinkAdded=function(t){var n=this._ctrl.getLinks();for(var r=0;r<n.length;r++)n[r].getUI().setActive(!1);t.setActive(!0),e("#"+f).append(t.getTabHTML()),e("#"+l).append(t.getContentHTML()),t.onHTMLAppended(),m(n),n.length>1?e("#"+p).removeClass("disabled"):e("#"+p).addClass("disabled")},v.prototype.c2pNodeLinkRemoved=function(t){e("#node-link-root-"+t._ctrl._id).remove(),e("#node-link-"+t._ctrl._id).remove()},v.prototype.c2pDisableDeleteNodeLinkButton=function(){e("#"+p).addClass("disabled")},v.prototype.c2pEnableDeleteNodeLinkButton=function(){e("#"+p).removeClass("disabled")},v.prototype.c2pPushModelStarted=function(){e("#node-progress-bar").addClass("progress-info progress-striped"),e("#node-progress-bar").removeClass("bar-success bar-danger"),e("#node-progress-bar").show()},v.prototype.c2pPushModelEndedWell=function(){e("#node-progress-bar").removeClass("progress-info progress-striped"),e("#node-progress-bar .bar").addClass("bar-success")},v.prototype.c2pPullModelStarted=function(){e("#node-progress-bar").addClass("progress-info progress-striped"),e("#node-progress-bar").removeClass("bar-success bar-danger"),e("#node-progress-bar").show()},v.prototype.c2pPullModelEndedWell=function(){e("#node-progress-bar").removeClass("progress-info progress-striped"),e("#node-progress-bar .bar").addClass("bar-success"),e("#prop-popup").modal("hide"),n.setText("Model updated successfully"),n.setType(n.SUCCESS),n.show(5e3)},v.prototype.c2pUnableToPush=function(t){e("#node-progress-bar").removeClass("progress-info progress-striped"),e("#node-progress-bar .bar").addClass("bar-warning"),e("#node-push-pull-error").html("Unable to <strong>push</strong> model. ("+t+")"),e("#node-push-pull-error").show("fast")},v.prototype.c2pUnableToPull=function(t){e("#node-progress-bar").removeClass("progress-info progress-striped"),e("#node-progress-bar .bar").addClass("bar-warning"),e("#node-push-pull-error").html("Unable to <strong>pull</strong> model. ("+t+")"),e("#node-push-pull-error").show("fast")},v});