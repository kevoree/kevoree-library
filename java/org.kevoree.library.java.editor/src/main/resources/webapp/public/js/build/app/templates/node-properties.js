define(["jadeRuntime"],function(e){return function(n){var r=[],i=n||{},s=i.initBy,o=i.nodeLinks,u=i.groups;return r.push('<div class="row-fluid"><div class="span4">Reachable from\n&nbsp<i title="Minimum selected = 1" class="icon-info-sign"></i></div><select id="initby-nodes" multiple="multiple">'),function(){var t=s;if("number"==typeof t.length)for(var n=0,i=t.length;n<i;n++){var o=t[n];r.push("<option"+e.attrs({value:o.name,selected:o.selected?"selected":null,"class":["initby-node"]},{value:!0,selected:!0})+">"+e.escape((e.interp=o.name)==null?"":e.interp)+"</option>")}else{var i=0;for(var n in t){i++;var o=t[n];r.push("<option"+e.attrs({value:o.name,selected:o.selected?"selected":null,"class":["initby-node"]},{value:!0,selected:!0})+">"+e.escape((e.interp=o.name)==null?"":e.interp)+"</option>")}}}.call(this),r.push('</select><div id="node-links-container" class="well"><ul id="node-links-tabs" class="nav nav-tabs"><li class="pull-right"><div class="btn-group"><button id="node-link-delete" class="btn btn-danger disabled"><i class="icon-trash icon-white"></i></button><button id="node-link-add" class="btn btn-info"><i class="icon-plus icon-white"></i></button></div></li>'),function(){var t=o;if("number"==typeof t.length)for(var n=0,i=t.length;n<i;n++){var s=t[n];r.push(null==(e.interp=s.tabHTML)?"":e.interp)}else{var i=0;for(var n in t){i++;var s=t[n];r.push(null==(e.interp=s.tabHTML)?"":e.interp)}}}.call(this),r.push('</ul><div id="node-links-contents" class="tab-content">'),function(){var t=o;if("number"==typeof t.length)for(var n=0,i=t.length;n<i;n++){var s=t[n];r.push(null==(e.interp=s.contentHTML)?"":e.interp)}else{var i=0;for(var n in t){i++;var s=t[n];r.push(null==(e.interp=s.contentHTML)?"":e.interp)}}}.call(this),r.push('</div></div><div class="row-fluid"><button'+e.attrs({id:"node-push-action",type:"button","class":["btn","btn-inverse","span4",u.length==0?"disabled":null]},{"class":!0,type:!0})+'>Push</button><div class="span4"><select id="node-group-action" class="row-fluid">'),function(){var t=u;if("number"==typeof t.length)for(var n=0,i=t.length;n<i;n++){var s=t[n];r.push("<option"+e.attrs({value:s},{value:!0})+">"+e.escape((e.interp=s)==null?"":e.interp)+"</option>")}else{var i=0;for(var n in t){i++;var s=t[n];r.push("<option"+e.attrs({value:s},{value:!0})+">"+e.escape((e.interp=s)==null?"":e.interp)+"</option>")}}}.call(this),r.push("</select></div><button"+e.attrs({id:"node-pull-action",type:"button","class":["btn","btn-inverse","span4",u.length==0?"disabled":null]},{"class":!0,type:!0})+'>Pull</button></div><div id="node-push-pull-error" class="text-error hide"></div><div id="node-progress-bar" class="progress progress-info progress-stripped active row-fluid hide"><div class="bar"></div></div></div>'),r.join("")}});