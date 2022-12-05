package org.jetlinks.simulator.cmd.mqtt;

import org.jetlinks.simulator.cmd.CommonCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "mqtt",
        description = "Mqtt client operations",
subcommands = {
        MqttPublishCommand.class,
        ConnectMqttCommand.class,
        MqttAttachCommand.class
})
public class MqttOperationCommand extends CommonCommand implements Runnable {


    @Override
    public void run() {
        showHelp();
    }
}
