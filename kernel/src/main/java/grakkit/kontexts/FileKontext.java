package grakkit.kontexts;

import org.graalvm.polyglot.Source;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class  FileKontext extends Kontext {
    // The main path of this kontext, which ideally points to a JavaScript file
    public String main;

    // Creates a new file-based kontext from the given path
    public FileKontext(String props, String main, String root) {
        super(props, root);
        this.main = main;
    }

    // Executes this FileKontext
    @Override
    public void execute () throws IOException {
        File index = Paths.get(this.root).resolve(this.main).toFile();
        if (index.exists()) this.graalContext.eval(Source.newBuilder("js", index).mimeType("application/javascript+module").build());
        else index.createNewFile();
    }
}