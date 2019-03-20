package org.jetlinks.simulator.mqtt;

import com.alibaba.fastjson.JSONObject;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface MessageHandler {
    void handle(JSONObject message, MQTTSimulator.ClientSession session);
}
