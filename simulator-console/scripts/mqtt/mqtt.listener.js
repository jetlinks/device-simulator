
//内置变量
//simulator , listener

simulator.doOnComplete(function () {

})

//监听创建连接前
listener.onBefore(function (session) {


});


listener.onAfter(function (session) {

    session
        .subscribe("/read-property",0)
        .subscribe(function (msg) {
            var json = msg.payloadAsJson();
            var property= json.get("properties").get(0);

            var properties = {};
            properties[property]=(Math.random()*10000)/100;

            session.publish("/read-property-reply",{
                messageId:json.get('messageId'),
                deviceId:json.deviceId,
                success:true,
                properties:properties
            })

     });

});