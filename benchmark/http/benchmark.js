/**
 * JetLinks http 官方协议模拟器
 *  benchmark http --url http://127.0.0.1:8801 report=true reportLimit=100 interval=1000
 */

//绑定内置参数,否则匿名函数无法使用。
var $benchmark = benchmark;

//在jetlinks平台的产品ID
var productId = args.getOrDefault("productId", "simulator");
var deviceIdPrefix = args.getOrDefault("deviceIdPrefix", "mqtt-test-");
var secureKey = "test";

var $enableReport = "true" === args.getOrDefault("report", "true");
var $reportLimit = parseInt(args.getOrDefault("reportLimit", "100"));
var $reportInterval = parseInt(args.getOrDefault("interval", "1000"));


//创建连接之前动态生成用户名密码
function beforeConnect(index, options) {
    var clientId = deviceIdPrefix + index;
    options.setId(clientId);
}

//全部连接完成后执行
function onComplete() {
    if (!$enableReport) {
        return
    }
    //定时执行1s
    $benchmark
        .interval(function () {
            $benchmark.print("批量上报属性..");
            return $benchmark
                .randomConnectionAsync($reportLimit, reportProperties);
        }, $reportInterval)

}


function reportProperties(client) {
    //创建随机数据
    var data = {};
    // $benchmark.print("上报[" + client.getId() + "]属性");
    for (let i = 0; i < 10; i++) {
        data["temp" + i] = randomFloat(10, 30);
    }
    var msg = {
        "properties": data
    }

    //请求
    return client.requestAsync({
        method: "POST",
        headers: {
            "Authorization": "Bearer " + secureKey
        },
        path: createTopic(client, "/properties/read/report"),
        contentType:"application/json",
         body: $benchmark.toJson(msg)
    });

}

//根据jetlinks官方协议topic规则创建topic
function createTopic(client, topic) {
    return "/" + productId + "/" + client.getId() + topic;
}


//重点! 绑定函数到benchmark
benchmark
    .beforeConnect(beforeConnect)
    // .onConnected(onConnected)
    .onComplete(onComplete);