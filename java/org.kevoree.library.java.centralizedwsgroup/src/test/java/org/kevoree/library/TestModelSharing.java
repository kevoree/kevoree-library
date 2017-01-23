package org.kevoree.library;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.kevoree.Channel;
import org.kevoree.ComponentInstance;
import org.kevoree.ContainerNode;
import org.kevoree.ContainerRoot;
import org.kevoree.api.Context;
import org.kevoree.api.KevScriptService;
import org.kevoree.api.ModelService;
import org.kevoree.api.handler.ModelListenerAdapter;
import org.kevoree.api.handler.UUIDModel;
import org.kevoree.factory.DefaultKevoreeFactory;
import org.kevoree.factory.KevoreeFactory;
import org.kevoree.library.client.ClientFragment;
import org.kevoree.library.protocol.Protocol;
import org.kevoree.library.server.ServerFragment;
import org.kevoree.log.Log;
import org.kevoree.pmodeling.api.json.JSONModelLoader;
import org.kevoree.tools.test.KevoreeMocks;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.junit.Assert.assertNotNull;

/**
 *
 * Created by leiko on 1/16/17.
 */
public class TestModelSharing {

    private static final KevoreeFactory factory = new DefaultKevoreeFactory();

    private ModelService masterModelService;
    private ModelService clientModelService;
    private KevScriptService kevsService;
    private CentralizedWSGroup masterInstance;
    private CentralizedWSGroup clientInstance;

    @Before
    public void setUp() {
        createServices();
        createMasterFragment();
        createClientFragment();
    }

    private void createServices() {
        Log.set(Log.LEVEL_DEBUG);

        KevoreeFactory factory = new DefaultKevoreeFactory();
        JSONModelLoader loader = factory.createJSONLoader();
        InputStream modelStream = getClass().getResourceAsStream("/model-sharing.json");
        ContainerRoot model = (ContainerRoot) loader.loadModelFromStream(modelStream).get(0);
        factory.root(model);

        UUIDModel uuidModel = Mockito.mock(UUIDModel.class);
        Mockito.when(uuidModel.getModel()).thenReturn(model);

        masterModelService = Mockito.spy(KevoreeMocks.modelService()
                .nodeName("master")
                .currentModel(model)
                .build());

        clientModelService = Mockito.spy(KevoreeMocks.modelService()
                .nodeName("client")
                .currentModel(model)
                .build());

        kevsService = Mockito.mock(KevScriptService.class);
    }

    private void createMasterFragment() {
        masterInstance = Mockito.spy(CentralizedWSGroup.class);
        Mockito.when(masterInstance.isMaster()).thenReturn(true);
        Mockito.when(masterInstance.getMasterNet()).thenReturn("-");
        Mockito.when(masterInstance.getPort()).thenReturn(9000);
        Mockito.when(masterInstance.isReduceModel()).thenReturn(true);

        Context context = Mockito.mock(Context.class);
        Mockito.when(context.getPath()).thenReturn("/groups[sync]");
        Mockito.when(context.getNodeName()).thenReturn("master");
        Mockito.when(context.getInstanceName()).thenReturn("sync");

        Mockito.when(masterInstance.getContext()).thenReturn(context);
        Mockito.when(masterInstance.getModelService()).thenReturn(masterModelService);
        Mockito.when(masterInstance.getKevsService()).thenReturn(kevsService);
    }

    private void createClientFragment() {
        clientInstance = Mockito.spy(CentralizedWSGroup.class);
        Mockito.when(clientInstance.isMaster()).thenReturn(false);
        Mockito.when(clientInstance.getMasterNet()).thenReturn("lo.ip");
        Mockito.when(clientInstance.getPort()).thenReturn(9000);
        Mockito.when(clientInstance.isReduceModel()).thenReturn(true);

        Context context = Mockito.mock(Context.class);
        Mockito.when(context.getPath()).thenReturn("/groups[sync]");
        Mockito.when(context.getNodeName()).thenReturn("client");
        Mockito.when(context.getInstanceName()).thenReturn("sync");

        Mockito.when(clientInstance.getContext()).thenReturn(context);
        Mockito.when(clientInstance.getModelService()).thenReturn(clientModelService);
        Mockito.when(clientInstance.getKevsService()).thenReturn(kevsService);
    }

    @Test
    public void testRegistration() throws InterruptedException, IOException {
        ServerFragment master = new ServerFragment(masterInstance);
        ClientFragment client = new ClientFragment(clientInstance);

        Protocol.RegisterMessage registerMsg = client.register();
        master.register(registerMsg);

        Mockito.verify(masterModelService, Mockito.times(1)).update(Mockito.any(), Mockito.isNull());
    }

    @Test
    public void testErroneousPush() throws InterruptedException, IOException {
        ServerFragment master = new ServerFragment(masterInstance);
        ClientFragment client = new ClientFragment(clientInstance);

        // register "client" to "master"
        Protocol.RegisterMessage registerMsg = client.register();
        // register "client" to "master"
        master.register(registerMsg);
        // fake confirmation
        client.registered();

        // fake editor pushing erroneous model
        Protocol.PushMessage pushMsg = new Protocol.PushMessage("wrong model");
        master.push("1.2.3.4", pushMsg);

        Mockito.verify(masterModelService, Mockito.times(1)).update(Mockito.any(), Mockito.isNull());
    }

    @Test
    public void testGoodPush() throws InterruptedException, IOException {
        ServerFragment master = new ServerFragment(masterInstance);
        ClientFragment client = new ClientFragment(clientInstance);

        Protocol.RegisterMessage registerMsg = client.register();
        // register "client" to "master"
        master.register(registerMsg);
        // fake confirmation
        client.registered();

        InputStream goodModelInputStream = getClass().getResourceAsStream("/model-sharing-with-comps.json");
        String goodModel = IOUtils.toString(goodModelInputStream, Charset.defaultCharset());
        IOUtils.closeQuietly(goodModelInputStream);
        Protocol.PushMessage pushMsg = new Protocol.PushMessage(goodModel);

        // register some behavior on model updates to fake the real behavior
        masterModelService.registerModelListener(new ModelListenerAdapter() {
            @Override
            public void modelUpdated() {
                // fake broadcast of new model to all clients
                client.push(pushMsg);
            }
        });

        // fake "editor" pushing a good model
        master.push("1.2.3.4", pushMsg);

        Mockito.verify(masterModelService, Mockito.times(2)).update(Mockito.any(), Mockito.isNull());

        ContainerRoot newModel = masterModelService.getCurrentModel().getModel();
        ContainerNode masterNode = newModel.findNodesByID("master");
        ComponentInstance ticker = masterNode.findComponentsByID("ticker");
        assertNotNull(ticker);

        ContainerNode clientNode = newModel.findNodesByID("client");
        ComponentInstance printer = clientNode.findComponentsByID("printer");
        assertNotNull(printer);

        Channel chan = newModel.findHubsByID("chan");
        assertNotNull(chan);

        Mockito.verify(clientModelService, Mockito.times(1)).update(Mockito.any(), Mockito.isNull());
    }

    @Test
    public void testGoodPushOnNotYetRegisteredClient() throws InterruptedException, IOException {
        ServerFragment master = new ServerFragment(masterInstance);
        ClientFragment client = new ClientFragment(clientInstance);

        InputStream goodModelInputStream = getClass().getResourceAsStream("/model-sharing-with-comps.json");
        String goodModel = IOUtils.toString(goodModelInputStream, Charset.defaultCharset());
        IOUtils.closeQuietly(goodModelInputStream);
        Protocol.PushMessage pushMsg = new Protocol.PushMessage(goodModel);

        // fake "editor" pushing a good model
        master.push("1.2.3.4", pushMsg);

        Mockito.verify(masterModelService, Mockito.times(1)).update(Mockito.any(), Mockito.isNull());

        // fake master broadcasting to everyone
        client.push(pushMsg);

        ContainerRoot newModel = masterModelService.getCurrentModel().getModel();
        ContainerNode masterNode = newModel.findNodesByID("master");
        ComponentInstance ticker = masterNode.findComponentsByID("ticker");
        assertNotNull(ticker);

        ContainerNode clientNode = newModel.findNodesByID("client");
        ComponentInstance printer = clientNode.findComponentsByID("printer");
        assertNotNull(printer);

        Channel chan = newModel.findHubsByID("chan");
        assertNotNull(chan);

        Mockito.verify(clientModelService, Mockito.times(0)).update(Mockito.any(), Mockito.isNull());
    }

    @Test
    public void testPull() {
        ServerFragment master = new ServerFragment(masterInstance);
        String modelStr = master.pull("1.2.3.4");

        JSONModelLoader loader = factory.createJSONLoader();
        ContainerRoot model = (ContainerRoot) loader.loadModelFromString(modelStr).get(0);
        ContainerNode masterNode = model.findNodesByID("master");
        ContainerNode clientNode = model.findNodesByID("client");
        assertNotNull(masterNode);
        assertNotNull(clientNode);
    }
}
