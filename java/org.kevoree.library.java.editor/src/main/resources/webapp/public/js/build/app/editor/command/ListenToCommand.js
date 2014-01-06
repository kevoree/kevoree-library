define(["util/AlertPopupHelper","kevoree"],function(e,t){function n(){}function r(t,n){e.setHTML("Listening to "+n+"..."),e.setType(e.SUCCESS),e.show(3500),u(n,function(){t.close()})}function i(t,n){e.setText("New model received from ws://"+n),e.setType(e.SUCCESS),e.show(3500),u(n,function(){t.close()})}function s(t){e.setHTML("Unable to connect to "+t+".<br/> Aborting listening process..."),e.setType(e.ERROR),e.show(3500),a()}function o(){e.setHTML('"Listen to" aborted. <br/> Connection closed.'),e.setType(e.WARN),e.show(3500),a()}function u(e,t){$("#listen-to-content").html('Currently listening to <span class="text-info">'+e+"</span>"),$("#listen-to-close").show(),$("#listen-to-close").off("click"),$("#listen-to-close").on("click",t),$("#listen-to").hide()}function a(){$("#listen-to-content").empty(),$("#listen-to-close").hide(),$("#listen-to").show()}return n.prototype.execute=function(e,n){var u=new WebSocket("ws://"+n),a=new t.org.kevoree.loader.JSONModelLoader,f=new t.org.kevoree.serializer.JSONModelSerializer,l=new t.java.io.OutputStream;u.onmessage=function(t){var r=a.loadModelFromString(t.data).get(0);e.setModel(r),i(u,n)},u.onopen=function(){r(u,n)},u.onclose=function(){o()},u.onerror=function(){s(n)},e.setModelListener({onUpdates:function(){u.send(f.serialize(e.getModel()))}})},n});