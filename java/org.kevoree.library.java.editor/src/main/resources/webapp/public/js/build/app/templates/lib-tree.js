define(["jadeRuntime"],function(e){return function(n){var r=[],i=n||{},s=i.libz;return function(){var t=s;if("number"==typeof t.length)for(var n=0,i=t.length;n<i;n++){var o=t[n];r.push('<ul class="nav nav-list"><li class="nav-header cursor-pointer lib-tree-library"><i class="lib-subtree-icon icon-arrow-right icon-white"></i>'+e.escape((e.interp=o.name)==null?"":e.interp)+"</li>"),function(){var t=o.components;if("number"==typeof t.length)for(var n=0,i=t.length;n<i;n++){var s=t[n];s.type!="UnknownType"&&r.push("<li"+e.attrs({"data-entity":s.type,"class":["lib-item"]},{"data-entity":!0})+'><div class="lib-item-name">'+e.escape((e.interp=s.name)==null?"":e.interp)+"</div></li>")}else{var i=0;for(var n in t){i++;var s=t[n];s.type!="UnknownType"&&r.push("<li"+e.attrs({"data-entity":s.type,"class":["lib-item"]},{"data-entity":!0})+'><div class="lib-item-name">'+e.escape((e.interp=s.name)==null?"":e.interp)+"</div></li>")}}}.call(this),r.push("</ul>")}else{var i=0;for(var n in t){i++;var o=t[n];r.push('<ul class="nav nav-list"><li class="nav-header cursor-pointer lib-tree-library"><i class="lib-subtree-icon icon-arrow-right icon-white"></i>'+e.escape((e.interp=o.name)==null?"":e.interp)+"</li>"),function(){var t=o.components;if("number"==typeof t.length)for(var n=0,i=t.length;n<i;n++){var s=t[n];s.type!="UnknownType"&&r.push("<li"+e.attrs({"data-entity":s.type,"class":["lib-item"]},{"data-entity":!0})+'><div class="lib-item-name">'+e.escape((e.interp=s.name)==null?"":e.interp)+"</div></li>")}else{var i=0;for(var n in t){i++;var s=t[n];s.type!="UnknownType"&&r.push("<li"+e.attrs({"data-entity":s.type,"class":["lib-item"]},{"data-entity":!0})+'><div class="lib-item-name">'+e.escape((e.interp=s.name)==null?"":e.interp)+"</div></li>")}}}.call(this),r.push("</ul>")}}}.call(this),r.join("")}});