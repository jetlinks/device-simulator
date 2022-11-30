var $benchmark = benchmark;

var ByteArr = Java.type("byte[]");

var dataTypes = [];

var StringType = {
    decode: function (buffer) {
        var len = buffer.readInt();
        var bytes = new ByteArr(len);

        buffer.readBytes(bytes);
        return new java.lang.String(bytes);
    },

    encode: function (str, buffer) {
        var bytes = str.getBytes();
        if (bytes.length === 0) {
            buffer.writeInt(0);
        } else {
            buffer.writeInt(bytes.length);
            buffer.writeBytes(bytes);
        }
    },
    support: function (val) {
        return typeof val === 'string' ||
            (typeof val === 'java.lang.String')
    }
}
var DoubleType = {
    decode: function (buffer) {
        return buffer.readDouble();
    },
    encode: function (val, buffer) {
        buffer.writeDouble(val);
    },
    support: function (val) {
        return typeof val === 'number' ||
            (typeof val === 'java.lang.double') ||
            (typeof val === 'java.lang.Double')
    }
}
var IntType = {
    decode: function (buffer) {
        return buffer.readInt();
    },
    encode: function (val, buffer) {
        buffer.writeInt(val);
    },
    support: function (val) {
        return typeof val === 'int' ||
            (typeof val === 'java.lang.int') ||
            (typeof val === 'java.lang.Integer')
    }
}
var ArrayType = {
    decode: function (buffer) {
        var len = buffer.readShort();
        var arr = new java.util.ArrayList(parseInt(len));

        for (let i = 0; i < len; i++) {
            arr.add(decode(buffer));
        }

        return arr;

    },

    support: function (val) {
        return typeof val === 'array' ||
            typeof val === 'list' ||
            (typeof val === 'java.util.ArrayList')
    }
}
var ObjectType = {
    decode: function (buffer) {
        var len = buffer.readShort();
        var map = new java.util.HashMap(len);
        for (let i = 0; i < len; i++) {
            map.put(StringType.decode(buffer), decode(buffer));
        }
        return map;
    },
    encode: function (val, buffer) {
        buffer.writeShort(val.size());
        for (var key in val) {
            StringType.encode(key, buffer);
            encode(val[key], buffer)
        }
    }
}

dataTypes[0x04] = IntType;
dataTypes[0x0B] = StringType;
dataTypes[0x0D] = ArrayType;
dataTypes[0x0A] = DoubleType;
dataTypes[0x0E] = ObjectType;

function decode(buffer) {
    var t = buffer.readByte();
    var type = dataTypes[parseInt(t)];

    if (type) {
        return type.decode(buffer);
    }
    console.warn("unsupported decode type:{}", t)
    return null;
}

function encode(val, buffer) {
    for (let i = 0; i < dataTypes.length; i++) {
        let t = dataTypes[i];
        if (t && t.support && t.support(val)) {
            //type
            buffer.writeByte(i);

            t.encode(val, buffer);
            return;
        }
    }
    console.warn("unsupported encode value type {} :{}", typeof val, val);
}

return {
    encode:encode,
    decode:decode,
    IntType:IntType,
    StringType:StringType,
    ArrayType:ArrayType,
    DoubleType:DoubleType,
    ObjectType:ObjectType
}