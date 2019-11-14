package org.jetlinks.simulator.manage.operator;

import org.jetlinks.simulator.manage.TimerTaskManage;
import org.jetlinks.simulator.manage.message.AcceptableMessage;
import org.jetlinks.simulator.manage.template.TimerTaskTemplate;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface DeviceOperator {

    Mono<Boolean> sendMssage();

    Mono<Boolean> sendMssage(Mono<AcceptableMessage> message);

    Mono<TimerTaskManage> createTimerTask(Mono<TimerTaskTemplate> template);

    Mono<Boolean> reportMssage(Mono<AcceptableMessage> message);

//    void init();
}
