// src/utils/index.ts
export * from './files';
export * from './format';
export * from './regex';

export { isTaskId, isTick, taskId, tick } from '../core/ticker';

export const javaSourceMethodSkips = [
    'grakkit.DysfoldInterop.catchError',
    'com.oracle.truffle.polyglot.PolyglotFunctionProxyHandler.invoke',
    'jdk.proxy1.$Proxy75.run',
];
export const jsFileSkips = ['webpack/runtime/make'];
export const jsMethodSkips = ['catchAndLogUnhandledErrors'];