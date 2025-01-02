// src/types.ts
import { Opaque } from 'type-fest';

export type TaskId = Opaque<number, 'TaskId'>;
export type Tick = Opaque<number, 'Tick'>;
export type StringEventPriority = 'HIGH' | 'HIGHEST' | 'LOW' | 'LOWEST' | 'MONITOR' | 'NORMAL';
export type EventListener<T> = (event: T) => void;
export type ScriptEventListener<T> = { script: (event: T) => void; priority: StringEventPriority };

export type Polyglot = {
    eval: (...args: any[]) => any;
};

export type Queue = any; // Replace with a more specific type if possible
export type Message = any; // Replace with a more specific type if possible
export type jiFile = any; // Replace with a more specific type if possible
export type ogpContext = any; // Replace with a more specific type if possible
export type juLinkedList<T> = any; // Replace with a more specific type if possible

export interface TaskObject {
    baseTick: Tick;
    tick: Tick;
    fn: () => void;
    reset: boolean;
    id: TaskId;
}

export interface SourceMap {
    sources: string[];
    mappings: number[][][];
    startOffset: number;
}

export interface SourceLine {
    file: string;
    line: number;
}

export type basic =  | { [x in string]: basic } | basic[] | string | number | boolean | null | undefined | void;

export type record = {
    readonly children: record[];
    directory(): record;
    entry(): record;
    readonly exists: boolean;
    file(...sub: string[]): record;
    flush(): record;
    io: any;
    json(async?: false): any;
    json(async: true): Promise<any>;
    readonly name: string;
    readonly path: string;
    readonly parent: record;
    read(async?: false): string;
    read(async: true): Promise<string>;
    remove(): record;
    readonly type: 'folder' | 'file' | 'none';
    write(content: string, async?: false): record;
    write(content: string, async: true): Promise<record>;
};

export interface GrakkitConfig {
    main: string;
    initialize: boolean;
    verbose: boolean;
    pluginName: string;
}