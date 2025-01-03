// src/platform/paper/command.ts

import {catchAndLogUnhandledError} from '../../core/error';
import {env} from './index';

export function command(options: {
    name: string
    message?: string
    aliases?: string[]
    execute?: (sender: any, ...args: string[]) => void
    namespace?: string
    permission?: string
    tabComplete?: (sender: any, ...args: string[]) => string[]
  }) {
    switch (env.name) {
      case 'bukkit': {
        env.content.plugin.register(
          options.namespace || env.content.plugin?.getName() || 'grakkit',
          options.name,
          options.aliases || [],
          options.permission || '',
          options.message || '',
          (sender: any, label: string, args: string[]) => {
            catchAndLogUnhandledError(() => {
              if (!options.permission || sender.hasPermission(options.permission)) {
                options.execute && options.execute(sender, ...args)
              } else {
                sender.sendMessage(options.message || '')
              }
            }, `An error occured while attempting to execute the "${label}" command!`)
          },
          (sender: any, alias: string, args: string[]) => {
            return (
              catchAndLogUnhandledError(
                () => (options.tabComplete && options.tabComplete(sender, ...args)) || [],
                `An error occured while attempting to tab-complete the "${alias}" command!`
              ) ?? []
            )
          }
        )
        break
      }
    //   case 'minestom': {
    //     const command = new env.content.Command(options.name)
    //     command.addSyntax(
    //       (sender, context) => {
    //         try {
    //           options.execute && options.execute(sender, ...context.getInput().split(' ').slice(1))
    //         } catch (error) {
    //           console.error(
    //             `An error occured while attempting to execute the "${options.name}" command!`
    //           )
    //           console.error(error.stack || error.message || error)
    //         }
    //       },
    //       env.content.ArgumentType.StringArray('tab-complete').setSuggestionCallback(
    //         (sender, context, suggestion) => {
    //           for (const completion of options.tabComplete(
    //             sender,
    //             ...context.getInput().split(' ').slice(1)
    //           ) || []) {
    //             suggestion.addEntry(new env.content.SuggestionEntry(completion))
    //           }
    //         }
    //       )
    //     )
    //     env.content.registry.register(command)
    //   }
    }
  }
  