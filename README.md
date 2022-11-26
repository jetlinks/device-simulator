# 设备模拟器

运行前请先安装`java8`.

## 交互式CLI

```bash
$ git clone https://github.com/jetlinks/device-simulator.git
$ cd device-simulator
$ ./run-cli.sh
```

## DEMO

### MQTT 官方协议模拟

在项目根目录启动模拟器后执行命令:
```bash
benchmark mqtt --size=5000 --name=mqtt --host=127.0.0.1 --port=1883 --script=benchmark/mqtt/benchmark.js
```


### TCP 官方协议模拟

在项目根目录启动模拟器后执行命令:
```bash
benchmark tcp --size=1 --name=tcp --host=127.0.0.1 --port=8801 --script=benchmark/tcp/benchmark.js
```