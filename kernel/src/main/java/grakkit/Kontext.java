package grakkit;

import java.util.LinkedList;

public class Kontext {
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
    }

    // Closes this kontext & removes it from the kontext registry
    public void destroy() {
        this.close();
        Grakkit.kontexts.remove(this);
    }

    // Executes this kontext by calling its entry point
    public void execute() throws Throwable {
        // do nothing
    }

    // Open's this kontext's graal context
    public void open() {
        this.ignoreMultithreadError = true;
        this.thisID = Kontext.nextID++;
        this.tickCount = 0;
        try {
            this.execute();
        } catch (Throwable e) {
            logError(e);
        }
    }

    // Executes the tick loop of the kontext
    public void tick() {
        this.tickCount++;
        this.tickHandle.execute(null);
    }

    public void logError(Throwable error) {
        if (this.tickCount > 100 && this.ignoreMultithreadError) {
            this.ignoreMultithreadError = false;
        }

        if (error instanceof IllegalStateException) {
            // Ignore the error when spinning up the environment. This happens more from
            // reloading.
            if (error.getMessage().contains("Multi threaded access requested by thread Thread")
                    && this.ignoreMultithreadError) {
                return;
            }

            error.printStackTrace();

            return;
        }

        error.printStackTrace();
    }
}