var $benchmark = benchmark;

var Types = require("benchmark/tcp/tcp-types.js");

var messageTypes = [];
var doOnReadProperty;


var ReadProperty = {
    handle: function (buffer, client, msgId) {
        var properties = Types.ArrayType.decode(buffer);
        console.log("read properties:{}", properties);
        var response = client.newBuffer();

        //0x05 回复读取属性指令
        response.writeByte(0x05);
        //时间
        response.writeLong($benchmark.now());
        //消息ID
        response.writeShort(msgId);
        //deviceId
        Types.StringType.encode(client.getId(), response);

        if (typeof doOnReadProperty === "undefined") {
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

        sendTo(client, response);

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

        return buffer.readUnsignedByte();
    },
    toString: function () {
        return "ACK";
    }
};

function sendTo(client, buffer) {
    var len = buffer.writerIndex();
    // print(client.getId() + " send 0x" + client.toHex(buffer))
    client.send(
        client.newBuffer().writeInt(len).writeBytes(buffer)
    )

}


return {
    reportProperty: function (client, properties) {
        var response = client.newBuffer();

        //0x03 上报属性
        response.writeByte(0x03);
        //时间
        response.writeLong($benchmark.now());
        //消息ID
        response.writeShort(0);
        //deviceId
        Types.StringType.encode(client.getId(), response);

        //encode 属性信息
        Types.ObjectType.encode(properties, response);

        sendTo(client, response);
    },
    handleFromServer: function (client, byteBuf) {
        //  print("handle downstream " + client.toHex(byteBuf));

        //长度头 忽略掉
        var len = byteBuf.readInt();

        var typeByte = byteBuf.readByte();

        // 消息类型
        var type = messageTypes[parseInt(typeByte)];
        // //时间戳
        var timestamp = byteBuf.readLong();
        // //消息id
        var msgId = byteBuf.readUnsignedShort();
        //设备ID
        var deviceId = Types.StringType.decode(byteBuf);

        if (type && type.handle) {
            type.handle(byteBuf, client, msgId);
            console.log("handle message type[" + typeByte + "(" + type + ")] msgId [" + msgId + "] ")
        } else {
            console.log("unhandled message type[" + typeByte + "] msgId [" + msgId + "] ")
        }


    },
    doOnReadProperty: function (callback) {
        doOnReadProperty = callback;
    },
    online: function (client, token) {
        var buffer = client.newBuffer();
        //类型 0x01
        buffer.writeByte(0x01);
        //时间
        buffer.writeLong($benchmark.now());
        //消息ID
        buffer.writeShort(0);

        //deviceId
        Types.StringType.encode(client.getId(), buffer);
        //token
        Types.StringType.encode(client.attribute("token").orElse(token), buffer);

        sendTo(client, buffer);
    }
}