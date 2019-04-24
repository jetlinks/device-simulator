# 设备模拟器

启动方式:
1. java启动类:`org.jetlinks.simulator.mqtt.MQTTSimulator`
2. docker:
```bash
docker run -v handler.js:/scripts/handler.js -it --rm jetlinks/device-simulator
```
3. jar包运行,[下载jar包](https://github.com/jetlinks/device-simulator/raw/master/dist/device-simulator.jar),
执行命令`java -jar device-simulator.jar`

# 配置
可通过环境变量或者启动参数设置一下配置
```bash
mqtt.address=127.0.0.1  #mqtt服务地址
mqtt.port=1883          #mqtt端口
mqtt.start=0            #模拟设备数量起点
mqtt.limit=100          #模拟设备数量
mqtt.enableEvent=false  #启动事件上报
mqtt.eventLimit=1000    #每次上报时间的最大设备数量
mqtt.eventRate=100000   #时间上报频率,毫秒
mqtt.scriptFile=./scripts/handler.js #消息处理脚本
mqtt.binds=192.168.10.10,192.168.10.11 #绑定网卡(模拟设备数量较多时建议配置多个虚拟网卡)
mqtt.bindPortStart=10000 # 指定绑定网卡时,端口的初始值,每个网卡依次递增

mqtt.ssl=false                          #是否开启ssl双向认证
mqtt.p12Path=./ssl/jetlinks-client.p12  #p12客户端证书地址
mqtt.p12Password=jetlinks               #证书密码
mqtt.cerPath=./ssl/jetlinks-server.cer  #信任ca证书
```

理论上,模拟设备数量小于绑定网卡数量*(65535 - bindPortStart).

添加虚拟网卡到wlp2s0命令:
```bash
 sudo ifconfig wlp2s0:1 192.168.10.11 up
```

注意: linux上默认限制了端口数量,可通过命令: 
`cat /proc/sys/net/ipv4/ip_local_port_range`
查看.可通过修改此文件加大端口限制.

 
# 模拟消息收发
模拟器通过js脚本来处理消息，默认脚本文件为:`./scripts/handler.js`,可通过
修改此脚本来实现自定义的消息处理
```js
//连接成功时
simulator.onConnect(function(session){
    
})

//绑定topic处理
simulator.bindHandler("/invoke-function", function (message, session) {
    var messageId = message.messageId;
    var functionId = message.function;
    session.sendMessage("/invoke-function-reply", JSON.stringify({
        messageId: messageId,
        functionId:functionId,
        output: "success",
        timestamp: new Date().getTime(),
        success: true
    }));
});

//当开启了事件上报时，定时(mqtt.eventRate)调用此回调发送事件数据,
// index: 事件索引，由mqtt.eventLimit配置
// session: mqtt连接会话
simulator.onEvent(function (index, session) {
    session.sendMessage(JSON.stringify({
        messageId: new Date().getTime() + "" + Math.round((Math.random() * 100000)),
        event: "temperature",
        timestamp: new Date().getTime(),
        data: ((Math.random() * 100) + 1).toFixed(2)
    }))
});
//自定义帐号密码生成
simulator.onAuth(function(auth,index){
    auth.setClientId("simulator-device-"+index);
    auth.setUsername("simulator-device-"+index);
    auth.setPassword("simulator-device-"+index);
});
```
