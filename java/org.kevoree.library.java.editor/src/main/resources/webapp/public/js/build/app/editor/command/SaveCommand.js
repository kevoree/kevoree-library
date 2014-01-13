define(["jquery","kevoree","util/AlertPopupHelper"],function(e,t,n){function i(){}var r=".save-popup";return i.prototype.execute=function(i,s){e("body").off(r),e("body").on("hidden"+r,"#save-popup",function(){e("#saved-model-link").hide()}),e("#save").modal("show");if(s.getModel()){e("#save-popup").modal({show:!0});try{var o=new t.org.kevoree.serializer.JSONModelSerializer,u=JSON.parse(o.serialize(s.getModel()));e.ajax({type:"post",url:"save/"+i,data:{model:u},dataType:"json",success:function(t){e("#save-popup-text").html("Your model has been successfully uploaded to the server."),e("#saved-model-link").attr("href",t.href),e("#saved-model-link").show("fast")},error:function(){e("#save-popup-text").html("Something went wrong while uploading your model.. :(")}})}catch(a){e("#save-popup-text").html("Something went wrong while uploading your model.. :("),console.error("SaveCommand ERROR: "+a.message,s.getModel())}}else n.setType(n.WARN),n.setText("There is no model to save currently."),n.show(2e3)},i});