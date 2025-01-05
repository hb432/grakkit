package grakkit;

import java.util.LinkedList;

public class Queue {

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
}