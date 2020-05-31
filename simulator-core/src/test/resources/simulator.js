var simulator = _simulator;
var logger = _logger;

function handleMqtt(connection) {
    connection.subscribe("/property", function (message) {

    })
}

//创建mqtt连接
var createConnection = simulator
    .createAction("network", {"from": 0, "to": 1, "batch": 100})
    .generate(function (index) {
        return {
            "id": "test" + index,
            "type": "mqtt",
            "config": {
                "clientId": "test" + index,
                "username": "admin",
                "password": "admin"
            }
        };
    })
    .onConnected(handleMqtt)
    .onDisconnect(function (connection) {

    })


//获取连接并发送数据
var reportMessage = simulator
    .createAction("job")
    .handler(function () {
        return simulator
            .findConnection("limit 0,10") //获取10个设备
            .flatMap(function (connection) {
                return connection.publish("/report-property", JSON.stringify({
                    "properties": {"": 30.2}
                }))
            })
            .then();
    })

//定时发送消息
var timer = actions.createAction("timer", {
    cron: "0/10 * * * * ?"
})
    .action(reportMessage);

simulator.addAction(createConnection, timer);