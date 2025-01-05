package grakkit.kontext;

import org.graalvm.polyglot.Source;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
    public void execute() throws IOException {
        File index = Paths.get(this.root).resolve(this.main).toFile();

        if (index.exists()) {
            // Evaluate the JavaScript file if it exists
            this.graalContext.eval(Source.newBuilder("js", index).mimeType("application/javascript+module").build());
        } else {
            // Create the file if it doesn't exist
            index.createNewFile();
            // Add default content to the new file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(index))) {
                writer.write("console.log('JavaScript for Minecraft, with the Graal Kernel Kit.');\n");
                writer.write("require('grakkit/dist/index.js');\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.graalContext.eval(Source.newBuilder("js", index).mimeType("application/javascript+module").build());
        }
    }
}