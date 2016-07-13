package org.kevoree.library;


import java.util.Date;

import com.espertech.esper.client.*;
import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.Output;
import org.kevoree.annotation.Param;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.annotation.Update;
import org.kevoree.api.Callback;
import org.kevoree.api.CallbackResult;

import org.kevoree.library.data.StreamDoubleValue;
import org.kevoree.log.Log;

@ComponentType(version = 1)
public class EsperMonitor implements UpdateListener {

    @Param(defaultValue = "select * from StreamDoubleValue(portId='input1').win:length(2) having avg(value) > 6.0")
    public String cepStatement;

    @Param(defaultValue = "Event Occured")
    public String message;

    @KevoreeInject
    org.kevoree.api.Context context;

    @Output
    org.kevoree.api.Port out;

    private EPRuntime cepRT;

    private EPServiceProvider cep;

    private EPStatement cepStat;

    @Input
    public void in1(Object i) {

        Log.debug("receive " + i);

        StreamDoubleValue val = new StreamDoubleValue("input1", Double.parseDouble("" + i), new Date().getTime());

        cepRT.sendEvent(val);

    }

    @Input
    public void in2(Object i) {
        StreamDoubleValue val = new StreamDoubleValue("input2", Double.parseDouble("" + i), new Date().getTime());
        cepRT.sendEvent(val);

    }

    @Input
    public void in3(Object i) {
        StreamDoubleValue val = new StreamDoubleValue("input3", Double.parseDouble("" + i), new Date().getTime());
        cepRT.sendEvent(val);

    }


    @Start
    public void start() {
        Configuration cepConfig = new Configuration();
        cepConfig.addEventType("StreamDoubleValue", StreamDoubleValue.class.getName());
        cep = EPServiceProviderManager.getProvider("myCEPEngine", cepConfig);
        cepRT = cep.getEPRuntime();

        EPAdministrator cepAdm = cep.getEPAdministrator();
        cepStat = cepAdm.createEPL(cepStatement);
        cepStat.addListener(this);

    }

    @Stop
    public void stop() {
        cep.destroy();

    }

    @Update
    public void update() {
        EPAdministrator cepAdm = cep.getEPAdministrator();
        cepStat.removeAllListeners();
        cepStat.destroy();
        cepStat = cepAdm.createEPL(cepStatement);
        cepStat.addListener(this);
    }

    @Override
    public void update(EventBean[] arg0, EventBean[] arg1) {
        out.send(message, new Callback() {
            public void onError(Throwable arg0) {
            }

            public void onSuccess(CallbackResult arg0) {
            }
        });
    }

}

