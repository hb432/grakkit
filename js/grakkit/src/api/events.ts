// src/api/events.ts
import {catchAndLogUnhandledError, Grakkit} from '../core';
import { EventListener, ScriptEventListener, StringEventPriority } from '../type/types';
import {env} from "../platform/paper";

const createEventListener = () => new (Java.extend(Java.type('org.bukkit.event.Listener'), {}))() as any; // Simplified
const MainInstanceListener = createEventListener();

type RegisterEventType = <T>(
    eventClass: any,
    eventListenerArg: EventListener<T> | ScriptEventListener<T>,
    priority?: StringEventPriority,
    listener?: any
) => void;

export const registerEvent: RegisterEventType = (
    eventClass,
    eventListenerArg,
    priority = 'HIGHEST',
    listener = MainInstanceListener
) => {
    const eventListener = {
        priority: 'priority' in eventListenerArg ? eventListenerArg.priority : priority,
        script: 'script' in eventListenerArg ? eventListenerArg.script : eventListenerArg,
    };
    const name: string = eventClass.class.toString();
    env.content.manager.registerEvent(
        eventClass.class,
        listener,
        eventListener.priority,
        (x: any, signal: any) => {
            if (signal instanceof eventClass) {
                catchAndLogUnhandledError(() => {
                    eventListener.script(signal);
                }, `An error occurred while attempting to handle the "${name}" event!`);
            }
        },
        Grakkit.kontext
    );
};