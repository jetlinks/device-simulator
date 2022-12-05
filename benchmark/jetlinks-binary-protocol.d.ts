// @ts-ignore
import {ByteBuf, Connection} from "api.d.ts"
// @ts-ignore
import ArrayList = java.util.ArrayList;
// @ts-ignore
import HashMap = java.util.HashMap;

declare var protocol: Protocol;

interface DataType<T> {
    encode(val: T, buffer: ByteBuf);

    decode(buffer: ByteBuf): T;
}

interface Protocol {
    types: Types,

    createReportProperty(client: Connection, properties: object): ByteBuf;

    doOnSend(callback: (client: Connection, buffer: ByteBuf) => {}): void;
}

declare class Types {
    StringType: DataType<string>;
    IntType: DataType<number>;
    ArrayType: DataType<ArrayList<any>>;
    ObjectType: DataType<HashMap<any, any>>;

}