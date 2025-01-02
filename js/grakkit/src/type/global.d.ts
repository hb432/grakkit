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
}