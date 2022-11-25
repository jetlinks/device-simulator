/**
 * JetLinks tcp 官方协议模拟器
 *
 *    benchmark tcp --host=127.0.0.1 --port=8801 --script=demo/tcp/benchmark.js report=true reportLimit=100 interval=1000
 */

var protocol = require("benchmark/tcp/jetlinks-protocol.js");


var $enableReport = "true" === args.getOrDefault("report", "true");
var $reportLimit = parseInt(args.getOrDefault("reportLimit", "100"));
var $reportInterval = parseInt(args.getOrDefault("interval", "1000"));

//绑定内置参数,否则匿名函数无法使用。
var $benchmark = benchmark;
//平台配置的密钥
var secureKey = "test";

//平台下发读取属性指令时
protocol.doOnReadProperty(function (properties) {

    $benchmark.print("读取属性:"+properties);

    let data = new java.util.HashMap();

    properties.forEach(function (property) {
        data.put(property, randomFloat(20, 30))
    });

    return data;
})

//全部连接完成后执行
function onComplete() {
    if (!$enableReport) {
        return;
    }
    // 定时执行1s
    $benchmark
        .interval(function () {
            //随机获取100个连接然后上报属性数据
            return $benchmark.randomConnectionAsync($reportLimit, reportTcpProperty);
        }, $reportInterval)

}


//单个连接创建成功时执行
function onConnected(client) {

    //上线
    protocol.online(client, secureKey);

    //订阅读取属性
    client
        .handlePayload(function (buf) {
            protocol.handleFromServer(client, buf.getByteBuf());
        });

}

//随机上报数据
function reportTcpProperty(client) {
    var data = new java.util.HashMap();
    for (let i = 0; i < 1; i++) {
        data['temp' + i] = randomFloat(10, 30);
    }
    protocol.reportProperty(client, data);
}


//重点! 绑定函数到benchmark
benchmark
    //.beforeConnect(beforeConnect)
    .onConnected(onConnected)
    .onComplete(onComplete);