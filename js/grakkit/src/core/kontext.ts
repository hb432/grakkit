// src/core/kontext.ts
import { Message, Polyglot, Queue } from '../type/types';

export class KernelKontext {
    kontext: any;
    isKontextActive: boolean = false;
    static readonly engine: any = {};
    readonly hooks: Queue = [];
    readonly messages: Message[] = [];
    props: string = '';
    root: string = '';
    tickFn: () => void = () => {};
    onCloseFn: () => void = () => {};
    loggerFn: (error: any) => void = () => {};
    readonly tasks: Queue = [];

    constructor(root: string, props: string) {
        this.root = root;
        this.props = props;
    }

    close(): void {
        this.isKontextActive = false;
        this.onCloseFn();
    }

    destroy(): void {
        this.close();
    }

    execute(): void {
        // do nothing
    }

    open(): void {
        this.isKontextActive = true;
    }

    tick(): void {
        this.tickFn();
    }

    logError(error: any): void {
        this.loggerFn(error);
    }

    setTickFn(fn: () => void): void {
        this.tickFn = fn;
    }

    setOnCloseFn(fn: () => void): void {
        this.onCloseFn = fn;
    }

    setLoggerFn(fn: (error: any) => void): void {
        this.loggerFn = fn;
    }
}