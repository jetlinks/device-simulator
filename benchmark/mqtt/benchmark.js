/**
 * JetLinks mqtt 官方协议模拟器
 *  benchmark mqtt --host=127.0.0.1 --port=8801 --script=demo/tcp/benchmark.js report=true reportLimit=100 interval=1000
 */

//绑定内置参数,否则匿名函数无法使用。
var $benchmark = benchmark;

//在jetlinks平台的产品ID
var productId = args.getOrDefault("productId", "simulator");
var deviceIdPrefix = args.getOrDefault("deviceIdPrefix", "mqtt-test-");

var $enableReport = "true" === args.getOrDefault("report", "false");
var $reportLimit = parseInt(args.getOrDefault("reportLimit", "5000"));
var $reportInterval = parseInt(args.getOrDefault("interval", "600"));


/**
 * 创建连接前自定义配置
 * @see MqttClientOptions
 * @param index
 * @param options
 */
//创建连接之前动态生成用户名密码
function beforeConnect(index, options) {
    var clientId = "test-" + index;


    var username = "test";
    var password = "test";

    // 官方协议3.2分支默认使用明文传输
    //  username = username + "|" + now();
    //  password = md5(username + "|" + password);

    // options.setUsername("c0ff9451733b33225b9bf0879863c104");
    options.setUsername(username);
    options.setPassword(password);
    options.setClientId(clientId);
}

var reportJob;

//全部连接完成后执行
function onComplete() {
    if (!$enableReport) {
        return
    }
    // //定时执行1s
    // reportJob =  $benchmark
    //     .interval(function () {
    //         //随机获取1000个连接然后上报属性数据
    //         if ($benchmark.getConnectedSize() > 0) {
    //             return $benchmark
    //                 .randomConnectionAsync($reportLimit, reportProperties);
    //         }
    //     }, $reportInterval);

    // 并发上报
    reportJob = $benchmark.continuousConnection($reportLimit, reportProperties);

}

var count = new java.util.concurrent.atomic.AtomicLong(50 * 10000);

function reportProperties(client) {

    if (count.addAndGet(-1) < 0) {
        reportJob.dispose();
        return null;
    }
    if (count.get() % 100000 === 0) {
        $benchmark.print("剩余上报数量:" + count.get());
    }

    //创建随机数据
    var data = {};
    // $benchmark.print("上报[" + client.getId() + "]属性");
    for (let i = 0; i < 1; i++) {
        data["temp" + i] = randomInt(21, 33);
    }
    var msg = {
        "timestamp": now(),
        "properties": data,
        "headers": {
            "containsGeo": false,
            //忽略日志
            "ignoreLog": false,
            //忽略存储
            "ignoreStorage": false
        }
    }

    //推送mqtt
    return client.publishAsync(createTopic(client, "/properties/report"), 1, $benchmark.toJson(msg));

}

//单个连接创建成功时执行
function onConnected(client) {

    //订阅读取属性
    // client
    //     .subscribe("$share/g1/device/+/pi-agent/message/property/report",
    //         0,
    //         function (msg) {
    //             $benchmark.print("收到数据[" + client.getId() + "");
    //
    //             // handleReadProperty(client, msg.payload().toJsonObject())
    //         });
    //订阅读取属性
    client
        .subscribe("$share/g1/device/+/+/message/property/report",
            1,
            function (msg) {
                $benchmark.print("收到数据[" + client.getId() + "");

                // handleReadProperty(client, msg.payload().toJsonObject())
            });
    //订阅读取属性
    // client
    //     .subscribe("$share/g1/device/+/+/online,offline",
    //         1,
    //         function (msg) {
    //             $benchmark.print("收到数据[" + client.getId() + "");
    //
    //             // handleReadProperty(client, msg.payload().toJsonObject())
    //         });

}

//根据jetlinks官方协议topic规则创建topic
function createTopic(client, topic) {
    return "/" + productId + "/" + client.getId() + topic;
}


function handleReadProperty(client, msg) {
    var messageId = msg.getString("messageId");
    var properties = msg.getJsonArray("properties");

    $benchmark.print("读取[" + client.getId() + "]属性:" + properties);

    //创建随机数据
    var data = {};
    properties.forEach(function (property) {
        //随机数据
        data[property] = randomFloat(10, 30);
    });

    //构造回复数据
    var reply = {
        "deviceId": client.getId(),
        "messageId": messageId,
        "properties": data
    }
    //推送mqtt
    doPublish(client, "/properties/read/reply", reply)
}

function doPublish(client, topic, payload) {
    //推送mqtt
    client.publish(createTopic(client, topic), 0, $benchmark.toJson(payload));
}

//重点! 绑定函数到benchmark
benchmark
    .beforeConnect(beforeConnect)
    .onConnected(onConnected)
    .onComplete(onComplete);