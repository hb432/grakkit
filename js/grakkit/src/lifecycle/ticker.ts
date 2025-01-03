// src/core/ticker.ts
import { TaskId, TaskObject, Tick } from '../type/types';
import {Grakkit} from "../type/Grakkit"; // Import Grakkit from index.ts in core
import { createLifecycleHandler } from '../lifecycle/lifecycle';

const Context = Symbol('TickContext');

const lifecycle = createLifecycleHandler();

interface TickContext {
    tick: number;
    task: any | undefined;
    isActive: boolean;
    tickFns: ((tick: number) => void)[];
}

const nextTick = () => {
    const ctx = ticker[Context];
    if (!ctx.isActive) return;
    for (const fn of ctx.tickFns) {
        fn(ctx.tick);
    }
    ctx.tick += 1;
};

const createTicker = () => {
    const ctx: TickContext = { tick: 0, task: undefined, isActive: false, tickFns: [] };

    const start = () => {
        ctx.isActive = true;
        Grakkit.kontext.setTickHandle(nextTick);
    };

    const stop = async () => {
        ctx.isActive = false;
        if (ctx.task) ctx.task.cancel();
        return;
    };

    lifecycle.on('enable', {
        name: 'Ticker',
        callback: () => {
            start();
        },
    });

    return {
        [Context]: ctx,
        start,
        stop,
        getTick: () => ctx.tick,
        registerTickFn: (fn: (tick: number) => void) => {
            ctx.tickFns.push(fn);
        },
    };
};

export const ticker = createTicker();

export const isTick = (tick: number): tick is Tick => true;
export const isTaskId = (id: number): id is TaskId => true;
export const tick = (tick: number | Tick) => tick as Tick;
export const taskId = (id: number | TaskId) => id as TaskId;

export const createTickerTasks = () => {
    const context = { nextId: 0 };
    const taskIdMap = new Map<TaskId, TaskObject>();

    const remove = (id: number) => {
        if (!isTaskId(id)) return;
        taskIdMap.delete(id);
    };

    const add = (fn: () => void, baseTick: number, options?: { reset?: boolean; nextId?: number }) => {
        if (!isTick(baseTick)) return;
        const id = taskId(options?.nextId ?? context.nextId++);
        const targetTick = tick(ticker.getTick() + Math.max(baseTick, 1));
        taskIdMap.set(id, { baseTick, tick: targetTick, fn, reset: options?.reset || false, id });
        return id;
    };

    const runTask = (task: TaskObject) => {
        taskIdMap.delete(task.id);
        task.fn();
        if (task.reset) {
            add(task.fn, task.baseTick, { reset: task.reset, nextId: task.id });
        }
    };

    const run = (tick: number) => {
        if (!isTick(tick)) return;
        for (const [, task] of taskIdMap) {
            if (tick >= task.tick) {
                runTask(task);
            }
        }
    };

    lifecycle.on('enable', {
        name: 'Tasks',
        callback: () => {
            ticker.registerTickFn((tick) => run(tick));
        },
    });

    return { add, run, remove };
};

export const tickerTasks = createTickerTasks();