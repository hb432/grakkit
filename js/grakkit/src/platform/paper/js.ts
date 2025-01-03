import {command} from './command';
import {env} from './index';

const Class = Java.type('java.lang.Class');
const Iterable = Java.type('java.lang.Iterable');
const Iterator = Java.type('java.util.Iterator');
const Spliterator = Java.type('java.util.Spliterator');

const regex = {
   replace(string: string, pattern: string, replacement: string) {
      return string.replace(new RegExp(pattern, 'g'), replacement)
   },
   test(string: string, pattern: string) {
      return new RegExp(pattern).test(string)
   },
};

/** Converts array-like objects or iterators into arrays. */
export function array(object: any): any[] | null {
  if (object instanceof Array) {
    return [...object]
  } else if (object instanceof Iterable) {
    const output: any[] = []
    object.forEach((value: any) => {
      output.push(value)
    })
    return output
  } else if (object instanceof Iterator || object instanceof Spliterator) {
    const output: any[] = []
    object.forEachRemaining((value: any) => {
      output.push(value)
    })
    return output
  } else {
    return null
  }
}

/** Internal value used to represent circular object references in formatted output. */
export const circular = Symbol()

/** Formatting tools for script feedback. */
export const format = {
  /** Reformats complex error messages into layman-friendly ones. */
  error(error: any) {
    let type = 'Error'
    let message = `${error}`
    if (error.stack) {
      type = error.stack.split('\n')[0].split(' ')[0].slice(0, -1)
      message = error.message
      switch (type) {
        case 'TypeError':
          try {
            if (
              message.startsWith('invokeMember') ||
              message.startsWith('execute on foreign object')
            ) {
              const reason = message.split('failed due to: ')[1]
              if (reason.startsWith('no applicable overload found')) {
                const sets = reason
                  .split('overloads:')[1]
                  .split(']],')[0]
                  .split(')]')
                  .map((set) => {
                    return `(${set.split('(').slice(1).join('(')})`
                  })
                message = ['Invalid arguments! Expected:', ...sets].join('\n -> ').slice(0, -1)
              } else if (reason.startsWith('Arity error')) {
                message = `Invalid argument amount! Expected: ${reason.split('-')[1].split(' ')[2]}`
              } else if (reason.startsWith('UnsupportedTypeException')) {
                message = 'Invalid arguments!'
              } else if (reason.startsWith('Unknown identifier')) {
                message = `That method (${reason.split(': ')[1]}) does not exist!`
              } else if (reason.startsWith('Message not supported')) {
                message = `That method (${message.slice(14).split(')')[0]}) does not exist!`
              } else {
                message = message.split('\n')[0]
              }
            }
          } catch (error) {
            message = message.split('\n')[0]
          }
          break
        case 'SyntaxError':
          message = message.split(' ').slice(1).join(' ').split('\n')[0]
      }
    } else {
      error = `${error}`
      type = error.split(' ')[0].slice(0, -1)
      message = error.split(' ').slice(1).join(' ')
    }
    return `${type}: ${message}`
  },
  /** A pretty-printer for JavaScript objects. */
  output(object: any, condense?: boolean): string {
    if (condense === true) {
      if (object === circular) {
        return '...'
      } else {
        // @ts-expect-error
        const type = toString.call(object)
        switch (type) {
          case '[object Array]':
          case '[object Object]':
          case '[object Function]':
            return type.split(' ')[1].slice(0, -1)
          case '[foreign HostObject]':
            if (object instanceof Class && typeof object.getCanonicalName === 'function') {
              return object.getCanonicalName()
            } else if (typeof object.toString === 'function') {
              const string = object.toString()
              if (string) {
                return string
              }
            }
            const clazz = typeof object.getClass === 'function' ? object.getClass() : object.class
            if (clazz instanceof Class && typeof clazz.getCanonicalName === 'function') {
              return clazz.getCanonicalName()
            } else {
              return `${object}` || `${clazz}` || 'Object'
            }
          case '[foreign HostFunction]':
            return 'Function'
          default:
            switch (typeof object) {
              case 'bigint':
                return object.toString() + 'n'
              case 'function':
                return 'Function'
              case 'object':
                return object ? 'Object' : 'null'
              case 'symbol':
                return `<${object.toString().slice(7, -1)}>`
              case 'undefined':
                return 'undefined'
              default:
                return JSON.stringify(object)
            }
        }
      }
    } else {
      // @ts-expect-error
      switch (toString.call(object)) {
        case '[object Array]':
          return `[ ${[...object]
            .map((value: any) => format.output(object === value ? circular : value, true))
            .join(', ')} ]`
        case '[object Object]':
          return `{ ${[
            ...Object.getOwnPropertyNames(object).map((key) => {
              return `${key}: ${format.output(
                object === object[key] ? circular : object[key],
                true
              )}`
            }),
            ...Object.getOwnPropertySymbols(object).map((key) => {
              return `${format.output(key, true)}: ${format.output(
                object === object[key] ? circular : object[key],
                true
              )}`
            }),
          ].join(', ')} }`
        case '[object Function]':
          if (object instanceof Class && typeof object.getCanonicalName === 'function') {
            return object.getCanonicalName()
          } else if (typeof object.toString === 'function') {
            return regex.replace(object.toString(), '\\r', '')
          } else {
            return `${object}` || 'function () { [native code] }'
          }
        case '[foreign HostFunction]':
          return 'hostFunction () { [native code] }'
        default:
          const list = array(object)
          if (list) {
            return format.output(list)
          } else {
            return format.output(object, true)
          }
      }
    }
  },
}

command({
  name: 'js',
  permission: 'grakkit.command.js',
  message: `\xa7cYou do not have the required permission to run this command!`,
  execute(context, ...args) {
    // eslint-disable-next-line
    const self = globalThis.hasOwnProperty('self')
    // @ts-ignore - Investigate. Why is this a thing?
    self || (globalThis.self = context)
    let output: string
    try {
      const result = Polyglot.eval('js', args.join(' '))
      // @ts-ignore - Investigate. Why is this a thing?
      self || delete globalThis.self
      output = format.output(result)
    } catch (whoops) {
      // @ts-ignore - Investigate. Why is this a thing?
      self || delete globalThis.self
      output = format.error(whoops)
    }
    switch (env.name) {
      case 'bukkit':
        context.sendMessage(output)
        break
      case 'minestom':
        context.sendMessage(output)
        break
    }
  },
  tabComplete(context, ...args) {
    let body = ''
    let index = -1
    let scope: any = globalThis
    let valid = true
    let string: boolean | string = false
    let bracket: boolean | number | string = false
    let comment = false
    let property = ''
    const input = args.join(' ')
    while (valid && ++index < input.length) {
      const char = input[index]
      if (comment) {
        if (char === '*' && input[index + 1] === '/') {
          if (property) {
            input[index + 2] === ';' && (comment = false)
          } else {
            body = input.slice(0, index + 2)
            comment = false
          }
        }
      } else if (string) {
        if (char === '\\') {
          ++index
        } else if (char === string) {
          scope = {}
          string = false
        }
      } else if (bracket === true) {
        ;["'", '"', '`'].includes(char) ? (bracket = char) : (valid = false)
      } else if (typeof bracket === 'string') {
        switch (char) {
          case '\\':
            ++index
            break
          case bracket:
            bracket = -1
            break
          default:
            property += char
        }
      } else {
        switch (char) {
          case '/':
            switch (input[index + 1]) {
              case '/':
                valid = false
                break
              case '*':
                comment = true
                break
            }
            break
          case "'":
          case '"':
          case '`':
            bracket === -1 ? (valid = false) : (string = char)
            break
          case ')':
          case '{':
          case '}':
            bracket || (scope = {})
            break
          case '.':
          case '[':
            if (!bracket) {
              if (char === '.' || property) {
                body = input.slice(0, index + 1)
                // eslint-disable-next-line
                if (scope === globalThis && property === 'self' && !scope.hasOwnProperty('self')) {
                  scope = context
                } else {
                  scope = scope[property] || {}
                }
                char === '.' || (bracket = true)
                property = ''
              } else {
                body = input.slice(0, index + 1)
                scope = globalThis
              }
            }
            break
          case ']':
            bracket === -1 && (bracket = false)
            break
          case '\\':
            typeof bracket === 'string' ? ++index : (valid = false)
            break
          case ' ':
            property ? (valid = false) : (body = '')
            break
          default:
            if (regex.test(char, '[\\+\\-\\*\\/\\^=!&\\|\\?:\\(,;]')) {
              if (!bracket) {
                body = input.slice(0, index + 1)
                scope = globalThis
                property = ''
              }
            } else {
              property += char
            }
        }
      }
    }
    if (valid && scope && !(comment || string)) {
      const properties = [
        ...Object.getOwnPropertyNames(scope.constructor ? scope.constructor.prototype || {} : {}),
        ...Object.getOwnPropertyNames(scope),
      ]
      scope === globalThis && !properties.includes('self') && properties.push('self')
      return properties
        .sort()
        .filter((element) => element.toLowerCase().includes(property.toLowerCase()))
        .filter((name) => bracket || regex.test(name, '[_A-Za-z$][_0-9A-Za-z$]*'))
        .map((name) => {
          if (bracket) {
            return `${body}\`${regex.replace(name, '`', '\\`').split('\\').join('\\\\')}\`]`
          } else {
            return `${body}${name}`
          }
        })
    } else {
      return []
    }
  },
})