package org.jetlinks.simulator.cmd.mqtt;

import org.jetlinks.simulator.cmd.CommonCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "mqtt",
        description = "mqtt相关操作",
subcommands = {
        MqttPublishCommand.class,
        ConnectMqttCommand.class
})
public class MqttOperationCommand extends CommonCommand implements Runnable {


    @Override
    public void run() {
        showHelp();
    }
}
