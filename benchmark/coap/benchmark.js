/**
 * JetLinks coap 官方协议模拟器
 *
 * benchmark coap --url coap://192.168.32.180:8809 --script=../../benchmark/coap/benchmark.js
 */

//绑定内置参数,否则匿名函数无法使用。
var $benchmark = benchmark;

//echo -n '{"deviceId":"coap-test-001","properties":{"temperature":36.5}}' | coap post
// coap://localhost:8009/report-property

//在jetlinks平台的产品ID
var productId = args.getOrDefault("productId", "1829413289229496320");
var deviceIdPrefix = args.getOrDefault("deviceIdPrefix", "coap_");
//注意授权key必须是16个字符
//注意授权key必须是16个字符
//注意授权key必须是16个字符
var secureKey = "testtesttesttest";

var $enableReport = "true" === args.getOrDefault("report", "true");
var $reportLimit = parseInt(args.getOrDefault("reportLimit", "1"));
var $reportInterval = parseInt(args.getOrDefault("interval", "3000"));


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
        code: "POST",
        options: {

        },
        uri: createTopic(client, "/properties/report"),
        contentType: "application/json",
        payload: client.officialEncryptPayload(msg ,secureKey)
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