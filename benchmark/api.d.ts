import ArrayList = java.util.ArrayList;
import HashMap = java.util.HashMap;
import Mono = reactor.core.publisher.Mono;

/**
 * 可通过benchmark 命令的执行参数指定比如: benchmark mqtt --port 1883 --script ...  report=true reportLimit=100
 */
declare var args: java.util.HashMap<string, string>;

/**
 * @see 内置变量 benchmark
 */
declare var benchmark: Benchmark<ClientOptions | MqttClientOptions, MQTTConnection | TcpConnection>;

declare namespace java.util {

    interface HashMap<K, V> {

        get(key: K): any;

        put(key: K, value: V);

        putIfAbsent(key: K, value: V);

        getOrDefault(key: K,defaultValue:V): V;

    }

    interface ArrayList<T> {
        size(): number;

        add(T): void;

        forEach(callback: (e: T) => {}): void;

        //todo
    }
}


declare namespace reactor.core.publisher {

    interface Mono<T> {
        block(): T;

        subscribe(): Disposable;

        flatMap<R>(call: (t: T) => Mono<R>): Mono<R>;

        map<R>(call: (t: T) => R): Mono<R>;

        filter(call: (t: T) => boolean): Mono<T>;

        switchIfEmpty<R>(a: Mono<R>): Mono<R>;

    }

}

interface Java {
    type(str: string): object;
}

interface ClientOptions {

    setId(str: string): ClientOptions;

}

interface Connection {
    isAlive(): boolean;

}

interface MQTTConnection extends Connection {

    handle(callback: (msg: MqttMessage) => {}): Disposable

    subscribe(topic: string, qos: number, callback: (msg: MqttMessage) => {}): Disposable;

    publish(topic: string, qos: number, payload: object): void;

    publishAsync(topic: string, qos: number, payload: object): reactor.core.publisher.Mono<void>;

}

interface MqttQoS {
    value(): number;

    name(): string;
}

interface MqttMessage {
    topicName(): string;

    messageId(): number;

    isDup(): boolean;

    isRetain(): boolean;

    qosLevel(): MqttQoS,

    payload(): Buffer;
}


interface TcpConnection extends Connection {
    handlePayload(callback: (buffer: Buffer) => {}): Disposable;

    send(buf: ByteBuf): void;

    send(buf: ByteBuf): reactor.core.publisher.Mono<void>;
}

interface UDPConnection extends TcpConnection {


}


declare enum HttpMethod {
    GET, POST, PUT, DELETE, PATCH
}

declare class HttpHeader {
    Authorization: string;
}

declare class HttpRequest {
    method: HttpMethod;
    path: string;
    body: string;
    headers: HttpHeader;
    contentType: string;
}

declare interface HttpResponse {

}

interface HttpClient extends Connection {

    requestAsync(req: HttpRequest): Mono<HttpResponse>;

    request(req: HttpRequest): void;

}

/**
 * MQTT 配置API
 */
interface MqttClientOptions extends ClientOptions {
    setUsername(str: string): MqttClientOptions;

    setPassword(str: string): MqttClientOptions;

    setClientId(str: string): MqttClientOptions;
}

interface Buffer {
    getByteBuf(): ByteBuf;

    toJsonObject(): VertxJson;

    toJsonArray(): VertxJsonArray<any>;

    toString(): string;
}

/**
 * netty ByteBuf
 */
interface ByteBuf {

    readerIndex(): number;

    writerIndex(): number;

    writeByte(b: number): ByteBuf;

    writeShort(b: number): ByteBuf;

    writeLong(b: number): ByteBuf;

    writeInt(b: number): ByteBuf;

    writeLong(b: number): ByteBuf;

    writeBytes(b: ByteBuf): ByteBuf;

    readByte(): number;

    readUnsignedByte(): number;

    readShort(): number;

    readUnsignedShort(): number;

    readInt(): number;

    readUnsignedInt(): number;

    readLong(): number;

    readFloat(): number;

    readDouble(): number;

    getByte(offset: number): number;

    getUnsignedByte(offset: number): number;

    getShort(offset: number): number;

    getUnsignedShort(offset: number): number;

    getInt(offset: number): number;

    getUnsignedInt(offset: number): number;

    getLong(offset: number): number;

    getFloat(offset: number): number;

    getDouble(offset: number): number;
}


interface Disposable {
    dispose(): void;
}

interface Benchmark<O extends ClientOptions, C extends Connection> {

    print(template: string, args?: any[]);

    interval(callback: () => {}, interval: number): Disposable;

    delay(callback: () => {}, delay: number): Disposable;

    randomConnectionAsync(limit: number, callback): Mono<void> | any;

    onConnected(callback: (c: C) => {}): Benchmark<O, C>;

    onComplete(callback: () => {}): Benchmark<O, C>;

    beforeConnect(callback: (index: number, options: O) => {}): Benchmark<O, C>;

    doOnReload(d: Disposable): void;

    doOnDispose(d: Disposable): void;
}

interface VertxJsonArray<T> extends ArrayList<T> {
}

interface VertxJson {
    getString(key: string): string;

    getInt(key: string): string;

    getJsonArray(key: string): VertxJsonArray<any>;
}


/**
 * 对字符串进行md5,返回md5 hex字符串
 * @param str
 */
declare function md5(str: string): string;

/**
 * 返回当前时间戳
 */
declare function now(): number;

declare function newBuffer(): ByteBuf;

declare function require(scriptLocation: string): any;

declare function formatDate(date: number | Date, format: string): string;

declare function toJson(obj: any): string;

declare function parseJson(data: string | ByteBuf): HashMap<string, any>;

declare function toJavaType(obj: any): any;

declare function newArrayList(): ArrayList<any>;

declare function newHashMap(): HashMap<any, any>;

declare function randomFloat(min: number, max: number): number;

declare function randomInt(min: number, max: number): number;
