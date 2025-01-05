package grakkit.kontext;

import org.graalvm.polyglot.Source;

import java.io.IOException;

public class ScriptKontext extends Kontext {

    // The source code contained within this kontext
    public String script;

    // Creates a new script-based kontext from the given source
    public ScriptKontext(String props, String script, String root) {
        super(props, root);
        this.script = script;
    }

    // Executes this ScriptKontext
    @Override
    public void execute () throws IOException {
        this.graalContext.eval(Source.newBuilder("js", this.script, "Script").mimeType("application/javascript+module").build());
    }
}