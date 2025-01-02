// import { Consumer } from 'java.util.function' // Assuming this is available or needs a suitable definition

type Throwable = any; // Generic type for Java's Throwable
type PolyglotValue = any; // Placeholder for org.graalvm.polyglot.Value
type GraalContext = any;
type URLClassLoader = any; // Placeholder for java.net.URLClassLoader
type Class<T> = any; // Placeholder for java.lang.Class

/**
 * Represents a message within the Grakkit context system.
 */
export interface KMessage {
    topic: string;
    payload: any;
}

/**
 * Represents a callback function in the Grakkit context, potentially with error handling.
 */
export class JSCallback<T> {
    constructor(context: Kontext, errorMode?: boolean);
    register(callback: Consumer<T> | null): void;
    execute(value: T, errorMode?: boolean): void;
    executeVoid(value: T): void;
}

/**
 * Represents a queue of tasks or hooks within a Grakkit context.
 */
export class Queue<T> {
    constructor(context: Kontext);
    release(): void;
}

/**
 * Represents a JavaScript context managed by Grakkit.
 */
export class Kontext {
    /** The GraalVM context associated with this Grakkit context. */
    graalContext: GraalContext;

    /** The Polyglot engine used for all contexts. */
    static readonly engine: any;

    /** All registered unload hooks tied to this context. */
    readonly hooks: Queue<() => void>;

    /** All queued messages created by this context. */
    readonly messages: KMessage[];

    /** Properties of this context. */
    props: string;

    /** The root directory of this context. */
    root: string;

    /** The tick function of this context. */
    tickHandle: JSCallback<void>;

    /** The onClose function of this context. */
    onCloseHandle: JSCallback<void>;

    /** The logger function of this context. */
    loggerHandle: JSCallback<JSError>;

    /** All queued tasks linked to this context. */
    readonly tasks: Queue<() => void>;

    /** The unique ID of this context (used for minimizing multithreading error spam). */
    readonly thisID: number;

    /** The current tick count of this context. */
    tickCount: number;

    /** Whether to ignore multithreading errors after the initial setup. */
    ignoreMultithreadError: boolean;

    /**
     * Creates a new Grakkit context.
     * @param props The properties for this context.
     * @param root The root directory for this context.
     */
    constructor(props: string, root: string);

    /** Closes this context's GraalVM context. */
    close(): void;

    /** Closes this context and removes it from the context registry. */
    destroy(): void;

    /** Executes this context by calling its entry point. */
    execute(): void;

    /** Opens this context's GraalVM context. */
    open(): void;

    /** Sends a message to all listeners of a specific topic. */
    sendToListeners(message: KMessage): void;

    /** Publishes a message, sending it to listeners if the topic exists. */
    publishMessage(message: KMessage): void;

    /** Publishes all queued messages. */
    publishMessages(): void;

    /** Executes the tick loop for this context. */
    tick(): void;

    /** Logs an error, handling PolyglotExceptions and other exceptions. */
    logError(error: Throwable): void;

    /** Sets the tick handler function for this context. */
    setTickHandle(tickHandle: Consumer<void> | null): void;

    /** Sets the on close handler function for this context. */
    setOnCloseHandle(onCloseHandle: Consumer<void> | null): void;

    /** Sets the logger handler function for this context. */
    setLoggerHandle(loggerHandle: Consumer<JSError> | null): void;
}

/**
 * Represents a specialized Grakkit context loaded from a file.
 */
export class FileGrakkitKontext extends Kontext {
    constructor(props: string, root: string, main: string);
}

/**
 * Represents the configuration for Grakkit.
 */
export interface GrakkitConfig {
    main: string;
    // Add other configuration properties as needed based on your 'GrakkitConfig.java'
}

/**
 * Provides the core API for interacting with the Grakkit environment.
 */
export interface GrakkitAPI {
    /** All registered cross-context channels. */
    topics: Map<string, JSCallback<any>[]>;

    /** The context running on the main thread. */
    kernelKontext: FileGrakkitKontext;

    /** All kontexts created with the context management system. */
    kontexts: Kontext[];

    /** All registered class loaders. */
    classLoaders: Map<string, URLClassLoader>;

    /** Current Grakkit configuration. */
    config: GrakkitConfig;

    /** Closes all open instances. */
    close(): void;

    /** Initializes the Grakkit Environment. */
    init(root: string): void;

    /**
     * Locates the base URL for the given class. This method tries to
     * find the source location (JAR or directory) where the class is loaded from.
     * @param clazz The class whose location should be resolved.
     * @return The URL of the class's source location, or null if not found.
     */
    locateClassSource(clazz: Class<any>): any | null; // Using 'any' for URL

    /** Executes the task loop for all contexts. */
    tick(): void;

    /** Updates the current ClassLoader to one that supports GraalJS. */
    patch(loader: any): void; // Using 'any' as the specific Loader type isn't provided
}