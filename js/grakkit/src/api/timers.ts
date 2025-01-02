// src/api/timers.ts
import {createCatchAndLogUnhandledErrorHandler, tickerTasks} from '../core';

const baseTimer = (callback: () => void, delay: number, options?: Parameters<(typeof tickerTasks)['add']>[1]) => {
    const modifier = delay / 50;
    return tickerTasks.add(createCatchAndLogUnhandledErrorHandler(callback, 'Unhandled timer'), modifier, options as any);
};

export const setTimeout = (callback: () => void, delay: number) => baseTimer(callback, delay);
export const setInterval = (callback: () => void, delay: number) => baseTimer(callback, delay, { reset: true } as any);
export const setImmediate = (callback: () => void) => setTimeout(callback, 0);
export const clearTimeout = (id: number) => tickerTasks.remove(id);

const initializeTimers = () => {
    // @ts-expect-error
    globalThis.setTimeout = setTimeout;
    // @ts-expect-error
    globalThis.setInterval = setInterval;
    // @ts-expect-error
    globalThis.setImmediate = setImmediate;
    // @ts-ignore
    globalThis.clearTimeout = clearTimeout;
    // @ts-ignore
    globalThis.clearInterval = clearTimeout;
};

initializeTimers();