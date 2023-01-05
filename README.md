# 设备模拟器

运行前请先安装`java8`.

## 交互式CLI

```bash
$ git clone https://github.com/jetlinks/device-simulator.git
$ cd device-simulator
$ mvn clean package -DskipTests
$ java \
   -Dfile.encoding=UTF-8 \
   -Xmx1G \
   -Dsimulator.max-ports=50000 \
   "-Dsimulator.network-interfaces=.*" \
   -jar ${PWD}/simulator-cli/target/simulator-cli.jar
```

JVM参数说明:

1. `-Dsimulator.network-interfaces`: 指定可用的网卡,正则表达式. 如: eth0|eth1
2. `-Dsimulator.max-ports`: 指定可用的端口数量,默认为50000

## DEMO

### MQTT 官方协议模拟

在项目根目录启动模拟器后执行命令:
```bash
benchmark mqtt --size=5000 --name=mqtt --host=127.0.0.1 --port=1883 --script=benchmark/mqtt/benchmark.js
```

注意: 默认的`clientId`格式在`benchmark/mqtt/benchmark.js`中定义,默认为`mqtt-test-{index}`.
可修改代码或者通过命令参数`deviceIdPrefix=mqtt-test-`来修改.


### TCP 官方协议模拟

在项目根目录启动模拟器后执行命令:
```bash
benchmark tcp --size=1 --id=tcp-test-{index} --name=tcp --host=127.0.0.1 --port=8801 --script=benchmark/tcp/benchmark.js
```

注意: `--id=tcp-test-{index}`参数,表示在平台的tcp设备id格式为`tcp-test-{index}`,如:`tcp-test-0`

### 常见问题

1. 连接提示`no further information ...`,请在启动或者创建连接时指定网卡信息 `benchmark --interface 192....`