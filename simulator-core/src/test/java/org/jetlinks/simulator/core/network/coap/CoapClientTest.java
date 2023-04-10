package org.jetlinks.simulator.core.network.coap;

import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
class CoapClientTest {


    @Test
    void test() {

        CoapOptions opts = new CoapOptions();
        opts.setBasePath("coap://192.168.33.53:8802");
        opts.setId("test");

        CoapClient client = new CoapClient(opts);

        CoapResponse response = client
                .advancedAsync(
                        CoAP.Code.POST,
                        "/report-property",
                        "{\"deviceId\":\"coap-test\"}",
                        "application/json",
                        Collections.emptyMap()
                )
                .block(Duration.ofSeconds(1));

        System.out.println(response);
    }

}