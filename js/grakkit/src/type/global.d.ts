import { JavaTypes } from '@yam-js/graal-type-introspection'
import { YamApi } from './yamApi'

type JavaTypeKey<T extends string> = T | keyof JavaTypes

// Type Scenarios:
// Java.type('org.bukkit.Bukkit') -> JavaTypes['org.bukkit.Bukkit']
// Java.type<typeof Bukkit>('org.bukkit.Bukkit') -> typeof Bukkit
// Java.type('org.bukkit.DoesNotExist') -> any
// Java.type('org.') -> The parameter should be fully typed.

export type Java = {
    // NOTE: We need to support both string and keyof JavaTypes here, without
    // sacrificing type safety. Ideally, the developer should see all the
    // possible keys in the parameter list, but still be able to pass a non-existing
    // key
    type: <Return = any, Key extends string = string>(
        name: JavaTypeKey<Key>
    ) => Key extends keyof JavaTypes ? JavaTypes[Key] : Return
    extend: any
}

export type Polyglot = {
    eval: <T = any>(...args: any[]) => T
}

declare global {
    const Java: Java
    const Polyglot: Polyglot
    const Yam: YamApi
    const console: {
        // memory: any;
        assert(condition?: boolean, ...data: any[]): void;
        clear(): void;
        count(label?: string): void;
        countReset(label?: string): void;
        debug(...data: any[]): void;
        dir(item?: any, options?: any): void;
        // dirxml(...data: any[]): void;
        error(...data: any[]): void;
        // exception(message?: string, ...optionalParams: any[]): void;
        group(...data: any[]): void;
        groupCollapsed(...data: any[]): void;
        groupEnd(): void;
        info(...data: any[]): void;
        log(...data: any[]): void;
        // table(tabularData?: any, properties?: string[]): void;
        time(label?: string): void;
        timeEnd(label?: string): void;
        timeLog(label?: string, ...data: any[]): void;
        // timeStamp(label?: string): void;
        // trace(...data: any[]): void;
        warn(...data: any[]): void;
    };
}