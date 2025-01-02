// src/utils/async.ts
import {Grakkit} from "../../core";

const UUID = Java.type('java.util.UUID');
const aux = `${__dirname}/index.js`;
import { basic, record, jiFile } from '../../type/types';
import {Kontext} from "../../type/GrakkitAPI";
import {env} from "./index";
import {file} from "../../util";

const kontext = Kontext.engine;

export const desync = {
    async provide(provider: (data: any) => any | Promise<any>) {
        try {
            const { data, uuid } = JSON.parse(Grakkit.kontext.props);
            try {
                kontext.emit(uuid, JSON.stringify({ data: await provider(data), status: true }));
            } catch (error) {
                kontext.emit(uuid, JSON.stringify({ data: error, status: false }));
            }
        } catch (error) {
            throw "The current context's metadata is incompatible with the desync system!";
        }
    },
    async request(path: string | record | jiFile, data: basic = {}) {
        const script = file(path)
        if (script.exists) {
            const uuid = UUID.randomUUID().toString()
            const promise = kontext.on(uuid)
            kontext.create('file', file(path).io.getAbsolutePath(), JSON.stringify({ data, uuid })).open()
            const response = JSON.parse(await promise)
            if (response.status) return response.data as basic
            else throw response.data
        } else {
            throw 'That file does not exist!'
        }},
    shift(script: (...args: any[]) => any | Promise<any>) {
        switch (env.name) {
            case 'bukkit':
                return new Promise((resolve, reject) => {
                    env.content.server.getScheduler().runTaskAsynchronously(env.content.plugin,
                        new env.content.Runnable(async () => {
                            try {
                                resolve(await script());
                            } catch (error) {
                                reject(error);
                            }
                        })
                    );
                });
            case 'minestom':
                // eslint-disable-next-line
                return new Promise(async (resolve, reject) => {
                    try {
                        resolve(await script());
                    } catch (error) {
                        reject(error);
                    }
                });
            default:
                return script();
        }
    },
};

export function chain<A, B extends (input: A, loop: (input: A) => C) => any, C extends ReturnType<B>>(
    input: A,
    handler: B
): C {
    const loop = (input: A): C => {
        try {
            return handler(input, loop);
        } catch (error) {
            throw error;
        }
    };
    return loop(input);
}