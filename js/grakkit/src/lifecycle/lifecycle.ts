// src/api/lifecycle.ts
import {asyncCatchAndLogUnhandledError} from '../core';
import {config} from "../api/config";
import {Grakkit} from "../type/Grakkit";
const logVerbose = (...args: any[]) => {
    if (config.verbose) {
        console.log(...args);
    }
};
console.log('haha');
// logging
export const createLifecycleHandler = () => {
    const instances = new Map<string, Map<number, any>>();
    let nextId = 0;
    let isEnabled = false;
    
    console.log('createLifecycleHandler');

    const executeCallbacks = async (type: 'disable' | 'enable') => {
        const group = instances.get(type);
        if (!group) return;
        console.log(`Executing ${type} callbacks`);
        for (let i = 1; i <= 5; i++) {
            const priorityItems = [...group.values()].filter((item) => item.priority === i);
            for (const { callback, name } of priorityItems) {
                name && console.log(`${type === 'enable' ? 'Enabling' : 'Disabling'} ${name}`);
                await asyncCatchAndLogUnhandledError(
                    async () => await callback?.(),
                    `Error while executing ${type} callback`
                );
            }
        }
        console.log(`Finished executing ${type} callbacks`);
        instances.delete(type);
    };

    const enable = async () => {
        if (isEnabled) return;
        isEnabled = true;
        await executeCallbacks('enable');
    };

    const reload = async () => {
        logVerbose('Reloading Grakkit');
        await executeCallbacks('disable');
        Grakkit.reload();
        logVerbose('Finished reloading Grakkit');
    };

    const on = (name: 'disable' | 'enable', config: any): (() => void) => {
        if (name === 'enable' && isEnabled) {
            config.callback();
            return () => undefined;
        }
        const id = nextId++;
        const callbacks = instances.get(name) ?? new Map();
        callbacks.set(id, { priority: 3, ...config });
        instances.set(name, callbacks);
        return () => delete callbacks[id];
    };

    return { enable, reload, on };
};