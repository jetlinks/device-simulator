package org.jetlinks.simulator.core.network.coap;

import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
class CoapClientTest {


    @Test
    void test() {

        CoapOptions opts = new CoapOptions();
        opts.setBasePath("coap://192.168.32.180:8809");
        opts.setId("test");

        CoapClient client = new CoapClient(opts);

        Map<String, Object> request = new HashMap<>();
        request.put("code", CoAP.Code.POST);
        request.put("uri", "/1829413289229496320/coap_1/properties/report");
        request.put("payload", "{\n" +
                "  \"deviceId\":\"coap_1\",\n" +
                "  \"properties\": {\n" +
                "    \"temp\":321.49\n" +
                "  }\n" +
                "}");
        request.put("contentType","application/json");
        request.put("secureKey","testtesttesttest");

        CoapResponse response = client
                .requestAsync(request)
                .block(Duration.ofSeconds(1));

        System.out.println(response);
    }

}