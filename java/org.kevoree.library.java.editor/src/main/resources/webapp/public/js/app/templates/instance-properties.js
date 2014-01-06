define(['jadeRuntime'], function(jade) {
return function anonymous(locals) {
var buf = [];
var locals_ = (locals || {}),name = locals_.name,dictionary = locals_.dictionary,fragments = locals_.fragments;buf.push("<div class=\"row-fluid\"><div class=\"row-fluid\"><div class=\"span4\">Instance name</div><input" + (jade.attrs({ 'id':('instance-attr-name'), 'type':('text'), 'placeholder':('Name'), 'value':(name), "class": [('span8')] }, {"type":true,"placeholder":true,"value":true})) + "/>" + (null == (jade.interp = dictionary) ? "" : jade.interp) + "</div></div>");
if ( fragments.length > 0)
{
buf.push("<div class=\"row-fluid\"><div class=\"well\"><ul class=\"nav nav-tabs\">");
// iterate fragments
;(function(){
  var $$obj = fragments;
  if ('number' == typeof $$obj.length) {

    for (var i = 0, $$l = $$obj.length; i < $$l; i++) {
      var fragment = $$obj[i];

buf.push("<li" + (jade.attrs({ "class": [((i==0)?'active':null)] }, {"class":true})) + "><a" + (jade.attrs({ 'href':('#fragment-props-'+fragment.name), 'data-toggle':('tab') }, {"href":true,"data-toggle":true})) + ">" + (jade.escape((jade.interp = fragment.name) == null ? '' : jade.interp)) + "</a></li>");
    }

  } else {
    var $$l = 0;
    for (var i in $$obj) {
      $$l++;      var fragment = $$obj[i];

buf.push("<li" + (jade.attrs({ "class": [((i==0)?'active':null)] }, {"class":true})) + "><a" + (jade.attrs({ 'href':('#fragment-props-'+fragment.name), 'data-toggle':('tab') }, {"href":true,"data-toggle":true})) + ">" + (jade.escape((jade.interp = fragment.name) == null ? '' : jade.interp)) + "</a></li>");
    }

  }
}).call(this);

buf.push("</ul><div class=\"tab-content\">");
// iterate fragments
;(function(){
  var $$obj = fragments;
  if ('number' == typeof $$obj.length) {

    for (var i = 0, $$l = $$obj.length; i < $$l; i++) {
      var fragment = $$obj[i];

buf.push("<div" + (jade.attrs({ 'id':('fragment-props-'+fragment.name), "class": [('tab-pane'),((i==0)?'active':null)] }, {"class":true,"id":true})) + ">" + (null == (jade.interp = fragment.attributesHTML) ? "" : jade.interp) + "</div>");
    }

  } else {
    var $$l = 0;
    for (var i in $$obj) {
      $$l++;      var fragment = $$obj[i];

buf.push("<div" + (jade.attrs({ 'id':('fragment-props-'+fragment.name), "class": [('tab-pane'),((i==0)?'active':null)] }, {"class":true,"id":true})) + ">" + (null == (jade.interp = fragment.attributesHTML) ? "" : jade.interp) + "</div>");
    }

  }
}).call(this);

buf.push("</div></div></div>");
};return buf.join("");
};
});
