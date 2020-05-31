package org.jetlinks.simulator.defaults.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.simulator.core.aciton.ActionType;

@AllArgsConstructor
@Getter
public enum DefaultActionType implements ActionType {
    timer("定时任务"),
    script("脚本"),
    connection("创建连接")
    ;

    private final String name;

    @Override
    public String getId() {
        return name();
    }

}
