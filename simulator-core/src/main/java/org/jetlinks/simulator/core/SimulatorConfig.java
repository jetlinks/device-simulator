package org.jetlinks.simulator.core;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.Values;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SimulatorConfig {

    private String id;

    //类型
    private String type;

    //网络配置
    private Network network;

    //运行配置
    private Runner runner;

    //监听器
    private List<Listener> listeners;


    @Getter
    @Setter
    public static class Runner {
        private List<String> binds;

        private int total = 1;

        private int startWith = 0;

        private int batch = 100;
    }

    @Getter
    @Setter
    public static class Network {
        private Map<String, Object> configuration =new HashMap<>();

        public Network with(String key,Object value){
            configuration.put(key,value);

            return this;
        }
    }

    @Getter
    @Setter
    public static class Listener  {
        private String id;
        private String type;
        private Map<String, Object> configuration =new HashMap<>();

        public Listener id(String id){
            this.id=id;
            return this;
        }

        public Listener type(String value){
            this.type=value;
            return this;
        }

        public Listener with(String key,Object value){
            configuration.put(key,value);

            return this;
        }

    }
}
