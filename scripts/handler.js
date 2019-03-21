simulator.bindHandler("/invoke-function", function (message, session) {
    var messageId = message.messageId;
    var functionId = message.function;

    if (functionId === 'mockChildConnect') {
        var deviceId = message.args[0];
        session.sendMessage("/child-device-connect", JSON.stringify({
            messageId: new Date().getTime(),
            childDeviceId: deviceId,
            timestamp: new Date().getTime(),
            success: true
        }))
    }

    session.sendMessage("/invoke-function-reply", JSON.stringify({
        messageId: messageId,
        output: "success",
        timestamp: new Date().getTime(),
        success: true
    }))
});

simulator.bindHandler("/read-property", function (message, session) {
    var messageId = message.messageId;

    session.sendMessage("/read-property-reply", JSON.stringify({
        messageId: messageId,
        timestamp: new Date().getTime(),
        values: {"name": "1234"},
        success: true
    }))
});

//子设备操作
simulator.bindChildHandler("/read-property", function (message, session) {
    var messageId = message.messageId;

    session.sendChilDeviceMessage("/read-property-reply", message.deviceId, JSON.stringify({
        messageId: messageId,
        timestamp: new Date().getTime(),
        values: {"name": "3456"},
        success: true
    }))
});


simulator.onEvent(function () {
    return JSON.stringify({
        messageId: new Date().getTime() + "" + Math.round((Math.random() * 100000)),
        event: "temperature",
        timestamp: new Date().getTime(),
        data: ((Math.random() * 100) + 1).toFixed(2)
    })

});