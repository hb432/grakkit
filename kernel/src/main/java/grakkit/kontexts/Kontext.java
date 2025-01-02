package grakkit.kontexts;

import grakkit.*;
import grakkit.api.DysfoldInterop;
import grakkit.api.JSCallback;
import grakkit.api.JSError;
import grakkit.kontexts.api.KMessage;
import grakkit.kontexts.api.Queue;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.function.Consumer;

public class Kontext {
    // The graal context associated with this grakkit kontext
    public Context graalContext;

    // The polyglot engine used for all kontexts
    public static final Engine engine = Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").build();

    // All registered unload hooks tied to this kontext
    public final Queue hooks = new Queue(this);

    // All queued messages created by this kontext
    public final LinkedList<KMessage> messages = new LinkedList<>();

    // Properties of this kontext
    public String props;

    // The root directory of this kontext
    public String root;

    // The ticker/handle of this kontext
    public JSCallback<Void> tickHandle = new JSCallback<Void>(this);

    // The onClose function of this kontext
    public JSCallback<Void> onCloseHandle = new JSCallback<Void>(this);

    // The logger function of this kontext
    public JSCallback<JSError> loggerHandle = new JSCallback<JSError>(this, true);

    // All queued tasks linked to this kontext
    public final Queue tasks = new Queue(this);

    // (Minimize multithreading error spam)
    public static int nextID = 0;
    public int thisID, tickCount;
    public boolean ignoreMultithreadError = false;

    // Creates a new kontext from the given filepath
    public Kontext(String props, String root) {
        this.props = props;
        this.root = root;
    }

    // Closes this kontext's graal context
    public void close() {
        this.ignoreMultithreadError = true;
        this.onCloseHandle.execute(null);
        Context graalContext = this.graalContext;
        this.hooks.release();
        graalContext.close();
    }

    // Closes this kontext & removes it from the kontext registry
    public void destroy() {
        this.close();
        Grakkit.kontexts.remove(this);
    }

    // Executes this kontext by calling its entry point
    public void execute() throws Throwable {
        // IDK do nothing
    }

    // Open's this kontext's graal context
    public void open() {
        this.ignoreMultithreadError = true;
        this.thisID = Kontext.nextID;
        this.tickCount = 0;
        this.graalContext = Context.newBuilder("js")
                .engine(Kontext.engine)
                .allowAllAccess(true)
                .allowExperimentalOptions(true)
                .option("js.nashorn-compat", "true")
                .option("js.commonjs-require", "true")
                .option("js.ecmascript-version", "14")
                .option("js.commonjs-require-cwd", this.root)
                .build();
        this.graalContext.getBindings("js").putMember("__interop", Value.asValue(new DysfoldInterop()));
        this.graalContext.getBindings("js").putMember("GKK", Value.asValue(this));
        try {
            this.execute();
        } catch (Throwable e) {
            logError(e);
        }
    }
    public void sendToListeners(KMessage message) {
        Grakkit.topics.get(message.topic).forEach(listener -> {
            try {
                listener.executeVoid(message.payload);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    public void publishMessage(KMessage message) {
        this.messages.remove(message);
        if (Grakkit.topics.containsKey(message.topic)) sendToListeners(message);
    }

    public void publishMessages () {
        new ArrayList<>(this.messages).forEach(this::publishMessage);
    }

    // Executes the tick loop of the kontext
    public void tick() {
        this.tickCount++;
        this.tickHandle.execute(null);
        this.tasks.release();
        publishMessages();
    }

    public void logError(Throwable error) {
        if (this.tickCount > 100 && this.ignoreMultithreadError)
            this.ignoreMultithreadError = false;

        if (error instanceof IllegalStateException) {
            // Ignore the error when initializing environment (common when reloading)
            if (error.getMessage().contains("Multi threaded access requested by thread Thread") && this.ignoreMultithreadError)
                return;
            error.printStackTrace();
        }

        if (error instanceof PolyglotException) {
            PolyglotException exception = (PolyglotException) error;
            try {
                this.loggerHandle.execute(new JSError(exception), true);
                return;
            } catch (Throwable e) {
                e.printStackTrace();
                return;
            }
        }
        error.printStackTrace();
    }
    // Finding: This is a `Consumer` as it works best with the reload configuration.
    // If we change it to `Value`, it breaks reloading.
    public void setTickHandle(Consumer<Void> tickHandle) {
        if (tickHandle != null) this.tickHandle.register(tickHandle);
    }
    public void setOnCloseHandle(Consumer<Void> onCloseHandle) {
        if (onCloseHandle != null) this.onCloseHandle.register(onCloseHandle);
    }
    public void setLoggerHandle(Consumer<JSError> loggerHandle) {
        if (loggerHandle != null) this.loggerHandle.register(loggerHandle);
    }
}