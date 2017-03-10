//package org.kevoree.library;
//
//import fr.braindead.wsmsgbroker.Response;
//import fr.braindead.wsmsgbroker.WSMsgBrokerClient;
//import org.kevoree.*;
//import org.kevoree.Channel;
//import org.kevoree.annotation.*;
//import org.kevoree.annotation.ChannelType;
//import org.kevoree.api.*;
//import org.kevoree.api.Port;
//import org.kevoree.log.Log;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@ChannelType(version = 1, description = "A Kevoree chan that uses an external remote WebSocket server to share messages.<br/>This server must implement a <a href=\"https://github.com/maxleiko/wsmsgbroker\">specific protocol</a>")
//public class WSChan implements ChannelDispatch {
//
//	@KevoreeInject
//	Context context;
//	@KevoreeInject
//	ChannelContext channelContext;
//	@KevoreeInject
//	ModelService modelService;
//
//	@Param(defaultValue = "/", optional = false)
//	private String path;
//	@Param(optional = false)
//	private int port;
//	@Param(optional = false)
//	private String host;
//
//	private Map<String, WSMsgBrokerClient> clients;
//
//	@Start
//	public void start() {
//		Log.debug("Start WSChan");
//		clients = new HashMap<>();
//		if (path == null) {
//			path = "";
//		}
//
//		ContainerRoot model = modelService.getPendingModel();
//		if (model == null) {
//			model = modelService.getCurrentModel();
//		}
//		Channel thisChan = (Channel) model.findByPath(context.getPath());
//		Set<String> inputPaths = Helper.getProvidedPortsPath(thisChan, context.getNodeName());
//		Set<String> outputPaths = Helper.getRequiredPortsPath(thisChan, context.getNodeName());
//
//		for (String path : inputPaths) {
//			// create input WSMsgBroker clients
//			createInputClient(path + "_" + context.getInstanceName());
//		}
//
//		for (String path : outputPaths) {
//			// create output WSMsgBroker clients
//			createOutputClient(path + "_" + context.getInstanceName());
//		}
//	}
//
//	@Stop
//	public void stop() {
//		Log.debug("Stop WSChan");
//		if (this.clients != null) {
//			clients.values().stream().filter(client -> client != null)
//					.forEach(fr.braindead.wsmsgbroker.WSMsgBrokerClient::close);
//			this.clients = null;
//		}
//	}
//
//	@Update
//	public void update() {
//		stop();
//		start();
//	}
//
//	@Override
//	public void dispatch(String o, final Callback callback) {
//		ContainerRoot model = modelService.getCurrentModel();
//		Channel thisChan = (Channel) model.findByPath(context.getPath());
//		Set<String> outputPaths = Helper.getRequiredPortsPath(thisChan, context.getNodeName());
//
//		// create a list of destination paths
//		Set<String> destPaths = new HashSet<>();
//		// process remote paths in order to add _<chanName> to the paths
//		// (WsMsgBroker protocol)
//		// add processed remote path to dest
//		destPaths.addAll(channelContext.getRemotePortPaths().stream()
//				.map(remotePath -> remotePath + "_" + context.getInstanceName()).collect(Collectors.toList()));
//		// add local connected inputs to dest
//		Set<String> providedPaths = Helper.getProvidedPortsPath(thisChan, context.getNodeName()).stream()
//				.map(s -> s + "_" + context.getInstanceName()).collect(Collectors.toSet());
//
//		destPaths.addAll(providedPaths);
//		// create the array that will store the dest
//		String[] dest = new String[destPaths.size()];
//		// convert list to array
//		destPaths.toArray(dest);
//
//		for (final String outputPath : outputPaths) {
//			WSMsgBrokerClient client = this.clients.get(outputPath + "_" + context.getInstanceName());
//			if (client != null) {
//				if (callback != null) {
//					client.send(o, dest, (from, o1) -> {
//						CallbackResult result = new CallbackResult();
//						result.setPayload(o1.toString());
//						result.setOriginChannelPath(context.getPath());
//						result.setOriginPortPath(outputPath);
//						callback.onSuccess(result);
//					});
//				} else {
//					client.send(o, dest);
//				}
//			} else {
//				createInputClient(outputPath + "_" + context.getInstanceName());
//			}
//		}
//	}
//
//	private void createInputClient(final String id) {
//		Log.debug("createInputClient : " + id);
//		this.clients.put(id, new WSMsgBrokerClient(id, host, port, path, true) {
//			@Override
//			public void onUnregistered(String s) {
//				Log.info("{} unregistered from remote server", id);
//			}
//
//			@Override
//			public void onRegistered(String s) {
//				Log.info("{} registered on remote server", id);
//			}
//
//			@Override
//			public void onMessage(Object o, final Response response) {
//				Callback cb = null;
//
//				if (response != null) {
//					cb = new Callback() {
//						@Override
//						public void onSuccess(CallbackResult o) {
//							if (o != null) {
//								response.send(o);
//							}
//						}
//
//						@Override
//						public void onError(Throwable throwable) {
//							response.send(throwable);
//						}
//					};
//				}
//
//				List<Port> ports = channelContext.getLocalPorts();
//				for (Port p : ports) {
//					p.send(o.toString(), cb);
//				}
//			}
//
//			@Override
//			public void onClose(int code, String reason) {
//				Log.error("Connection closed by remote server for {}", id);
//			}
//
//			@Override
//			public void onError(Exception e) {
//			}
//		});
//	}
//
//	private void createOutputClient(final String id) {
//		Log.debug("createOutputClient : " + id);
//		this.clients.put(id, new WSMsgBrokerClient(id, host, port, path, true) {
//			@Override
//			public void onUnregistered(String s) {
//				Log.debug("{} unregistered from remote server", id);
//			}
//
//			@Override
//			public void onRegistered(String s) {
//				Log.debug("{} registered on remote server", id);
//			}
//
//			@Override
//			public void onMessage(Object o, final Response response) {
//			}
//
//			@Override
//			public void onClose(int code, String reason) {
//				Log.debug("Connection closed by remote server for {}", id);
//			}
//
//			@Override
//			public void onError(Exception e) {
//			}
//		});
//	}
//}