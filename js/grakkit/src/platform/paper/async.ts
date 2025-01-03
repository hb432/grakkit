// src/utils/async.ts
import {Grakkit} from "../../type/Grakkit";
const UUID = Java.type('java.util.UUID');
const aux = `${__dirname}/index.js`;
import { basic, record, jiFile } from '../../type/types';
import {KontextAPI} from "../../type/GrakkitAPI";
import {env} from "./index";
import {file} from "../../util";
const Context = Java.type('org.graalvm.polyglot.Context') as any;
/** A system which simplifies asynchronous cross-context code execution. */
export const desync = {
    /** Provides the result to a desync request within an auxilliary file. If this method is called while not within a desync-compatible context, it will fail. */
    async provide(provider: (data: basic) => basic | Promise<basic>) {
      try {
        const { data, uuid } = JSON.parse(Grakkit.kontext.props)
        try {
          Grakkit.emit(uuid, JSON.stringify({ data: await provider(data), status: true }))
        } catch (error) {
          Grakkit.emit(uuid, JSON.stringify({ data: error, status: false }))
        }
      } catch (error) {
        throw "The current context's metadata is incompatible with the desync system!"
      }
    },
    /** Sends a desync request to another file. If said file has a valid desync provider, that provider will be triggered and a response will be sent back when ready. */
    async request(path: string | record | jiFile, data: basic = {}) {
      const script = file(path)
      if (script.exists) {
        const uuid = UUID.randomUUID().toString()
        const promise = Context.on(uuid)
        Context.create('file', file(path).io.getAbsolutePath(), JSON.stringify({ data, uuid })).open()
        const response = JSON.parse(await promise)
        if (response.status) return response.data as basic
        else throw response.data
      } else {
        throw 'That file does not exist!'
      }
    },
    /** Runs a task off the main server thread. */
    shift<X>(script: (...args: any[]) => X | Promise<X>) {
      switch (env.name) {
        case 'bukkit':
          return new Promise<X>((resolve, reject) => {
            env.content.server.getScheduler().runTaskAsynchronously(
              env.content.plugin,
              new env.content.Runnable(async () => {
                try {
                  resolve(await script())
                } catch (error) {
                  reject(error)
                }
              })
            )
          })
        case 'minestom':
          // eslint-disable-next-line
          return new Promise<X>(async (resolve, reject) => {
            try {
              resolve(await script())
            } catch (error) {
              reject(error)
            }
          })
      }
    },
  }

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