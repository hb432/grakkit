package grakkit.kontext.api;

import grakkit.kontext.Kontext;
import org.graalvm.polyglot.Value;

import java.util.LinkedList;

public class Queue {

    /** The internal queue list. */
    public final LinkedList<Value> list = new LinkedList<>();

    /** The parent kontext associated with this queue. */
    public final Kontext parent;

    /**
     * Constructs a new queue and associates it with the given parent kontext.
     *
     * @param parent the parent kontext
     */
    public Queue(Kontext parent) {
        this.parent = parent;
    }

    /**
     * Executes and clears this queue.
     */
    public void release() {
        new LinkedList<>(this.list).forEach(value -> {
            try {
                value.executeVoid();
            } catch (Throwable error) {
                this.parent.logError(error);
            }
            this.list.remove(value);
        });
    }
}