
//内置变量
//simulator , listener

simulator.doOnComplete(function () {

})

//监听创建连接前
listener.onBefore(function (session) {


    session.options.willTopic="/test";
    session.options.willMessage="123";


});

simulator.doOnComplete(function () {

    simulator.timer(function () {

        simulator
            .getSessions()
            .subscribe(function (session) {
                session.publish("/test/1234","test2");
            });

    },1000)

})

//监听连接后
listener.onAfter(function (session) {

    session.publish("/test/123","test");

});