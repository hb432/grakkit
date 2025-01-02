package grakkit.api;

import grakkit.kontexts.Kontext;

import java.util.function.Consumer;

public class JSCallback<Args> {
    public int id;
    public Consumer<Args> fn;
    public Kontext kontext;
    public boolean throwErrors = true;

    public JSCallback(Kontext kontext) {
        this.kontext = kontext;
    }

    public JSCallback(Kontext kontext, boolean throwErrors) {
        this.kontext = kontext;
        this.throwErrors = throwErrors;
    }

    public void register(Consumer<Args> fn) {
        this.fn = fn;
        this.id = this.kontext.thisID;
    }

    public void execute(Args value) {
        this.execute(value, false);
    }

    public void execute(Args value, boolean throwErrors) {
        if (!this.hasCallback()) {
            error("No callback registered.", throwErrors);
            return;
        }
        if (this.id != this.kontext.thisID) {
            error("Callback registered to a different kontext!", throwErrors);
            return;
        }
        try {
            this.fn.accept(value);
        } catch (Throwable e) {
            if (!this.throwErrors) this.kontext.logError(e);
            else throw e;
        }

    }

    private boolean hasCallback() {
        return this.fn != null;
    }

    private void error(String message) {
        if (throwErrors) throw new RuntimeException(message);
    }
    private void error(String message, boolean throwErrors) {
        if (throwErrors) throw new RuntimeException(message);
    }
}