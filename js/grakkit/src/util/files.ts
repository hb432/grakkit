// src/utils/files.ts
import { basic, record, jiFile } from '../type/types';
import {chain, desync} from "../platform/paper";

const Files = Java.type('java.nio.file.Files');
const JavaString = Java.type('java.lang.String');
const Paths = Java.type('java.nio.file.Paths');
const UUID = Java.type('java.util.UUID');
const aux = `${__dirname}/index.js`;

export function file(path: string | record | jiFile, ...more: string[]) {
    path = typeof path === 'string' ? path : 'io' in path ? path.path : path.getPath()
    const io = Paths.get(path, ...more)
        .normalize()
        .toFile()
    // @ts-ignore
    // @ts-ignore
    const record: record = {
        get children(): record[] {
            return record.type === 'folder' ? [...io.listFiles()].map((sub) => file(sub.getPath())) : []
        },
        directory() {
            if (record.type === 'none') {
                chain(io, (io, loop) => {
                    const parent = io.getParentFile()
                    parent && (parent.exists() || loop(parent))
                    io.mkdir()
                })
            }
            return record
        },
        entry() {
            record.type === 'none' && record.parent.directory() && io.createNewFile()
            return record
        },
        get exists() {
            return io.exists()
        },
        file(...path) {
            return file(io, ...path)
        },
        flush() {
            chain(io, (io, loop) => {
                const parent = io.getParentFile()
                parent &&
                parent.isDirectory() &&
                (parent.listFiles()[0] || (parent.delete() && loop(parent)))
            })
            return record
        },
        io,
        json(async?: boolean) {
            if (async) {
                return record.read(true).then((content) => JSON.parse(content))
            } else {
                try {
                    return JSON.parse(record.read())
                } catch (error) {
                    return null
                }
            }
        },
        get name() {
            return io.getName()
        },
        get parent() {
            return record.file('..')
        },
        get path() {
            // return RegExp.replace(io.getPath(), '(\\\\)', '/')
            // the above regex RegExp is not valid ts
            //fixed:
            return new RegExp(io.getPath().replace(/(\\)/g, '/')) + '';
        },
        read(async?: boolean) {
            if (async) {
                return desync.request(aux, { operation: 'file.read', path: record.path }) as Promise<string>
            } else {
                return record.type === 'file'
                    ? new JavaString(Files.readAllBytes(io.toPath())).toString()
                    : null
            }
        },
        remove() {
            chain(io, (io, loop) => {
                io.isDirectory() && [...io.listFiles()].forEach(loop)
                io.exists() && io.delete()
            })
            return record.flush()
        },
        get type() {
            return io.isDirectory() ? 'folder' : io.exists() ? 'file' : 'none'
        },
        //@ts-ignore
        write(content: string, async?: boolean) {
            if (async) {
                return desync
                    .request(aux, { content, operation: 'file.write', path: record.path })
                    .then(() => record)
            } else {
                record.type === 'file' && Files.write(io.toPath(), new JavaString(content).getBytes())
                return record
            }
        }
    }
    return record
}