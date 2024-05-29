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
    var clientId = deviceIdPrefix + index;

    var secureId = "test";
    var secureKey = "test";

    var username = secureId + "|" + now();
    var password = md5(username + "|" + secureKey);

    options.setUsername(username);
    options.setPassword(password);
    options.setClientId(clientId);
}

//全部连接完成后执行
function onComplete() {
    if (!$enableReport) {
        return
    }
    //定时执行1s
    $benchmark
        .interval(function () {
            //随机获取1000个连接然后上报属性数据
            if ($benchmark.getConnectedSize() > 0) {
                return $benchmark
                    .randomConnectionAsync($reportLimit, reportProperties);
            }
        }, $reportInterval)

}

var count = 200*10000;

function reportProperties(client) {
    count--;
    if (count < 0) {
        return null;
    }
    if (count % 100000===0) {
        $benchmark.print("剩余上报数量:"+count);
    }
    //创建随机数据
    var data = {};
    // $benchmark.print("上报[" + client.getId() + "]属性");
    for (let i = 0; i < 10; i++) {
        data["temp" + i] = randomInt(33, 35);
    }
    var msg = {
        "timestamp": now(),
        "properties": data,
        "headers": {
            "containsGeo": false,
            "ignoreLog": true,
            "ignoreStorage": false
        }
    }

    //推送mqtt
    return client.publishAsync(createTopic(client, "/properties/report"), 1, $benchmark.toJson(msg));

}

//单个连接创建成功时执行
function onConnected(client) {

    //订阅读取属性
    client
        .subscribe(createTopic(client, "/properties/read"),
            0,
            function (msg) {
                handleReadProperty(client, msg.payload().toJsonObject())
            });

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