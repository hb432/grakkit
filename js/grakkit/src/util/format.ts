// src/utils/format.ts
import { regex } from '.';

const circular = Symbol();

export const format = {
    error(error: any) {
        let type = 'Error';
        let message: string[] = [`${error}`]; // Always an array

        if (error.stack) {
            type = error.stack.split('\n')[0].split(' ')[0];
            message = [error.message]; // Convert to an array for consistent processing.

            switch (type) {
                case 'TypeError':
                    try {
                        if (message[0].startsWith('invokeMember') || message[0].startsWith('execute on foreign object')) {
                            const reason = message[0].split('failed due to: ')[1];
                            if (reason.startsWith('no applicable overload found')) {
                                const sets = reason
                                    .split('overloads: ')[1]
                                    .split(']],')
                                    .map((set: string) => `(${set.split('(').slice(1).join('(')})`);
                                message = ['Invalid arguments! Expected:', ...sets];
                            } else if (reason.startsWith('Arity error')) {
                                message = [`Invalid argument amount! Expected: ${reason.split('-')[1].split(' ')[1]}`];
                            } else if (reason.startsWith('UnsupportedTypeException')) {
                                message = ['Invalid arguments!'];
                            } else if (reason.startsWith('Unknown identifier')) {
                                message = [`That method (${reason.split(': ')[1]}) does not exist!`];
                            } else if (reason.startsWith('Message not supported')) {
                                message = [`That method (${message[0].slice(14).split(')')}) does not exist!`];
                            } else {
                                message = message[0].split('\n');
                            }
                        }
                    } catch (err) {
                        message = message[0].split('\n');
                    }
                    break;
                case 'SyntaxError':
                    message = error.message.split(' ').slice(1);
                    break;
            }
        } else {
            // Fallback if no stack is available.
            type = error.split(' ')[0];
            message = [`${error}`];
        }

        // Return as joined string for output:
        return `${type}: ${message.join('\n')}`;
    },

    output(object: any, condense?: boolean): string {
        if (condense === true) {
            if (object === circular) {
                return '...';
            } else {
                const type = toString.call(object);
                switch (type) {
                    case '[object Array]':
                    case '[object Object]':
                    case '[object Function]':
                        return type.split(' ')[1].slice(0, -1);
                    case '[foreign HostObject]':
                        if (typeof object.getCanonicalName === 'function') {
                            return object.getCanonicalName();
                        } else if (typeof object.toString === 'function') {
                            const string = object.toString();
                            if (string) {
                                return string;
                            }
                        }
                        const clazz = typeof object.getClass === 'function' ? object.getClass() : object.class;
                        if (typeof clazz.getCanonicalName === 'function') {
                            return clazz.getCanonicalName();
                        } else {
                            return `${object}` || `${clazz}` || 'Object';
                        }
                    case '[foreign HostFunction]':
                        return 'Function';
                    default:
                        switch (typeof object) {
                            case 'bigint':
                                return object.toString() + 'n';
                            case 'function':
                                return 'Function';
                            case 'object':
                                return object ? 'Object' : 'null';
                            case 'symbol':
                                return `<${object.toString().slice(7, -1)}>`;
                            case 'undefined':
                                return 'undefined';
                            default:
                                return JSON.stringify(object);
                        }
                }
            }
        } else {
            switch (toString.call(object)) {
                case '[object Array]':
                    return `[ ${[...object].map((value: any) => format.output(object === value ? circular : value, true)).join(', ')} ]`;
                case '[object Object]':
                    return `{ ${[...Object.getOwnPropertyNames(object).map((key) => {
                        return `${key}: ${format.output(object === object[key] ? circular : object[key], true)}`;
                    }),
                        ...Object.getOwnPropertySymbols(object).map((key) => {
                            return `${format.output(key, true)}: ${format.output(object === object[key] ? circular : object[key], true)}`;
                        }),
                    ].join(', ')} }`;
                case '[object Function]':
                    if (typeof object.getCanonicalName === 'function') {
                        return object.getCanonicalName();
                    } else if (typeof object.toString === 'function') {
                        return regex.replace(object.toString(), '\\r', '');
                    } else {
                        return `${object}` || 'function () { [native code] }';
                    }
                case '[foreign HostFunction]':
                    return 'hostFunction () { [native code] }';
                default:
                    const list = array(object);
                    if (list) {
                        return format.output(list);
                    } else {
                        return format.output(object, true);
                    }
            }
        }
    },
};

export function array(object: any): any[] | null {
    if (object instanceof Array) {
        return [...object];
    } else if (typeof object?.forEach === 'function') {
        const output: any[] = [];
        object.forEach((value: any) => {
            output.push(value);
        });
        return output;
    } else {
        return null;
    }
}