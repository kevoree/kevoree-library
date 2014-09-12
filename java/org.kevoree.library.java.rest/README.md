# Kevoree Rest Gateway

This component is a gateway to access Kevoree elements through a classical REST compliant API.

First of all, this gateway is composed by a component server which as to be deployed into the Kevoree platform, through a KevScript like :

```
add node0.rest : RestServer
```

Then additional components and channels can be added as usual

```
add node0.print : ConsolePrinter
add node0.ticker : Ticker
add hub0 : SyncBroadcast
bind node0.print.input hub0
bind node0.ticker.tick hub0
```

By default the server expose a rest interface through http://localhost:8090 (this port can be configure using KevScript).

This configure is usable has is using maven at the root of this project

```
mvn kev:run
```

# Plain HTTP POST Client

Then message can be injected as plain text using an HTTP POST client such as curl on unix system.

Using this API and using the previously initialised Kevoree Platform man can inject value into :

### channel

The REST url should take the form of http://<url>:<port>/channels/<channelName>
In post param you can provide your message payload.

Here is an exemple:

```
curl --data "externalData" http://localhost:8090/channels/hub0
```

### port

The REST url should take the form of http://<url>:<port>/components/<componentName>/<portName>
In post param you can provide your message payload.

Here is an exemple:

```
curl --data "externalData" http://localhost:8090/components/print/input
```

# Java Client

Additionally we provide in this package a Plain Java client usable outside of Kevoree.

This client (class : org.kevoree.library.client.KevoreeRestClient) be build using the constructor and an url parameter.

Then two methods allow to perform similar call to plain HTTP Client API. 

        KevoreeRestClient client = new KevoreeRestClient("http://localhost:8090/");
        System.out.println(client.send2Channel("hub0", "injectedFromExternalSource"));
        System.out.println(client.send2Port("print", "input", "injectedFromExternalSource"));

 

###### TODO synchronous call for inpect result
