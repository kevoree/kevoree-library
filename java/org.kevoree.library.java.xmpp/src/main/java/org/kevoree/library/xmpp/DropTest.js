function start() {
    var r = new java.lang.Runnable({
        run: function () {
            print("running...\n");
            Thread.sleep(5000);
            print("After")

        }
    });
    var Thread = Java.type("java.lang.Thread");
    var System = Java.type("java.lang.System");
    var th = new Thread(r);
    th.start();


    print(System);

}


function stop() {
    return "KevoreeStop";
}

function in_hello(p) {

}

