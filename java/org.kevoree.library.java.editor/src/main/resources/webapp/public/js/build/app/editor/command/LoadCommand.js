define(["jquery","util/ModelHelper","util/AlertPopupHelper","util/Config","util/Util","kevoree"],function(e,t,n,r,i,s){function u(){}var o=".load-file-command";return u.prototype.execute=function(t){function u(){e("#file").trigger("click"),e("#file").off(o),e("#file").on("change"+o,function(){var r=e("#file").get(0).files[0];e("#file").get(0).files.length>1&&console.warn("You have selected multiple files ("+e("#file").get(0).files[0].length+") so I took the first one in the list ("+e("#file").get(0).files[0].name+")");var i=new FileReader;i.onload=function(e){var i=JSON.parse(e.target.result),o=JSON.stringify(i),u=new s.org.kevoree.loader.JSONModelLoader,a=u.loadModelFromString(o).get(0);t.setModel(a),n.setText('Model "'+r.name+'" loaded successfully'),n.setType(n.SUCCESS),n.show(5e3)},i.readAsText(r),e(this).val(""),e("#file").off(o)})}function a(){var e=!0;if(window.localStorage){var t=window.localStorage.getItem(r.LS_CONFIRM_ON_LOAD);t!=undefined&&(e=i.parseBoolean(t))}return e}t.getModel()!=null&&a()?(n.setHTML("<p>Do you want to overwrite current model ?<br/>Any unsaved work will be lost.</p><div class='row-fluid'><button id='confirm-load-model' type='button' class='btn btn-mini btn-danger'>Load model</button><button id='keep-current-model' type='button' class='btn btn-mini btn-primary pull-right'>Keep model</button></div><small>You can disable this confirmation popup in <a href='#' id='disable-confirm-load'>settings</a></small>"),n.setType(n.WARN),n.show(),e("#confirm-load-model").off("click"),e("#confirm-load-model").on("click",function(){n.hide(),u()}),e("#keep-current-model").off("click"),e("#keep-current-model").on("click",function(){n.hide()}),e("#disable-confirm-load").off("click"),e("#disable-confirm-load").on("click",function(){e("#settings-popup").modal("show")})):u()},u});