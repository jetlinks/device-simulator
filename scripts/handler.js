simulator.bindHandler("/invoke-function", function (message, session) {
    var messageId = message.messageId;
    var functionId = message.functionId;

    session.sendMessage("/invoke-function-reply", JSON.stringify({
        messageId: messageId,
        output:"success",
        timestamp:new Date().getTime(),
        success: true
    }))
});

simulator.bindHandler("/read-property", function (message, session) {
    var messageId = message.messageId;

    session.sendMessage("/read-property-reply", JSON.stringify({
        messageId: messageId,
        timestamp:new Date().getTime(),
        values:{"name":"1234"},
        success: true
    }))
});