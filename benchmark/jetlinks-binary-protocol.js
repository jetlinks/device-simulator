var $benchmark = benchmark;

var Types = require("benchmark/jetlinks-binary-types.js");

var messageTypes = [];
var doOnReadProperty, _doOnSend;


var ReadProperty = {
    handle: function (buffer, client, msgId) {
        var properties = Types.ArrayType.decode(buffer);

        var response = newBuffer();

        //0x05 回复读取属性指令
        response.writeByte(0x05);
        //时间
        response.writeLong(now());
        //消息ID
        response.writeShort(msgId);
        //deviceId
        Types.StringType.encode(client.getId(), response);

        if (typeof doOnReadProperty === "undefined" || typeof _doOnSend == 'undefined') {
            //error
            response.writeBoolean(false);
            encode("unsupported", response)
            encode("请在脚本中设置protocol.doOnReadProperty", response)
        } else {
            try {
                //创建随机数据
                var data = doOnReadProperty(properties);
                //success
                response.writeBoolean(true);
                Types.ObjectType.encode(data, response);
            } catch (e) {
                response.writeBoolean(false);
                encode("unsupported", response)
                encode(e.message, response);
            }
        }

        _doOnSend(client, response);

    },
    toString: function () {
        return "ReadProperty";
    }
}

//读取属性
messageTypes[0x04] = ReadProperty;

//ACK
messageTypes[0x02] = {

    handle: function (buffer, client) {
        return ["OK", "NO_AUTH", "UNSUPPORTED"][parseInt(buffer.readUnsignedByte())];
    },
    toString: function () {
        return "ACK";
    }
};

function handleFromServer(client, byteBuf) {
    // $benchmark.print("handle [" + client.getId() + "] downstream: " + client.toHex(byteBuf));

    // 消息类型
    var typeByte = byteBuf.readByte();
    var type = messageTypes[parseInt(typeByte)];

    // //时间戳
    var timestamp = byteBuf.readLong();
    //消息id
    var msgId = byteBuf.readUnsignedShort();
    //设备ID
    var deviceId = Types.StringType.decode(byteBuf);

    if (type && type.handle) {
        if (typeByte === 0x02) {
            $benchmark.print("收到来自服务的设备[" + client.getId() + "]应答[" + type.handle(byteBuf, client, msgId) + ")]:序号 [" + msgId + "] ");
            return
        }
        $benchmark.print("收到来自服务的设备[" + client.getId() + "]指令[" + typeByte + "(" + type + ")]:序号 [" + msgId + "] ")
        type.handle(byteBuf, client, msgId);

    } else {
        $benchmark.print("不支持的设备 [" + client.getId() + "] 指令[" + typeByte + "]:序号 [" + msgId + "] ")
    }

}

var msgId = 1;
return {
    createReportProperty: function (client, properties) {
        var buffer = newBuffer();

        //0x03 上报属性
        buffer.writeByte(0x03);
        //时间
        buffer.writeLong(now());
        //消息ID
        buffer.writeShort(msgId++);
        //deviceId
        Types.StringType.encode(client.getId(), buffer);

        //encode 属性信息
        Types.ObjectType.encode(properties, buffer);

        return buffer;
    },
    createPing:function (client){
        var buffer = newBuffer();

        //0x00 心跳
        buffer.writeByte(0x00);

        return buffer;
    },
    handleFromServer: handleFromServer,
    doOnReadProperty: function (callback) {
        doOnReadProperty = callback;
    },
    doOnSend: function (callback) {
        _doOnSend = callback;
    },
    createOnline: function (client, token) {
        var buffer = client.newBuffer();
        //类型 0x01
        buffer.writeByte(0x01);
        //时间
        buffer.writeLong(now());
        //消息ID
        buffer.writeShort(0);

        //deviceId
        Types.StringType.encode(client.getId(), buffer);
        //token
        Types.StringType.encode(client.attribute("token").orElse(token), buffer);

        return buffer;
    },
    "types": Types
}