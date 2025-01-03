// src/core/error.ts
import { mapLineToSource } from './sourceMap';
import { javaSourceMethodSkips, jsFileSkips, jsMethodSkips } from '../util';
import {warn} from "./logger";

export const formatError = (error: any) => {
    const list: string[] = [];
    list.push(error.name);
    for (let i = 0; i < error.stack.length; i++) {
        const item = error.stack[i];
        if (item.javaFrame) {
            if (javaSourceMethodSkips.includes(`${item.source}.${item.methodName}`)) continue;
            list.push(` at ${item.source}.${item.methodName}(${item.fileName}:${item.line})`);
            continue;
        }
        const source = mapLineToSource(item.source, item.line);
        if (jsFileSkips.some((skip) => source.file.includes(skip))) continue;
        if (jsMethodSkips.includes(item.methodName)) continue;
        if (source.line === 0) continue;
        let methodName = item.methodName || '';
        if (item.methodName === ':=>') {
            methodName = '';
        }
        list.push(` at ${methodName} (${source.file}:${source.line}) [${item.source}:${item.line}]`);
    }
    return list.join('\n');
};
export const logError = (error: unknown, msg?: string) => {
    let jsError;
    try {
        // @ts-expect-error
        const errorType = error?.getClass?.()?.getName?.() ?? undefined;
        if (errorType?.includes('grakkit.JSError')) {
            jsError = error;
        } else {
            //@ts-expect-error
            jsError = __interop.catchError(() => {
                throw error;
            });
        }
        const errorMsg = formatError(jsError);
        msg && warn(msg);
        warn(errorMsg)
    } catch (thislogError) {
        warn('ERROR: There was an error logging an error. Please report to Grakkit. ', thislogError);
        // @ts-expect-error
        warn(logError.message, logError.stack);
        // @ts-expect-error
        warn('Original error: ', error?.name);
        // @ts-expect-error
        warn(error?.message, error?.stack);
    }
};

export const catchAndLogUnhandledError = <R, P extends any[]>(fn: (...arg: P) => R, msg: string): R | undefined => {
    try {
        // @ts-ignore
        return fn();
    } catch (error) {
        logError(error, msg);
    }
    return undefined
};

export const asyncCatchAndLogUnhandledError = async <T>(fn: () => Promise<T>, msg: string): Promise<T | undefined> => {
    try {
        return await fn();
    } catch (error) {
        logError(error, msg);
    }
    return undefined
};
export const createCatchAndLogUnhandledErrorHandler = <R, P extends any[]>(fn: (...arg: P) => R, msg: string) => (...args: P): R | undefined => {
    try {
        return fn(...args);
    } catch (error) {
        logError(error, msg);
    }
    return undefined
};