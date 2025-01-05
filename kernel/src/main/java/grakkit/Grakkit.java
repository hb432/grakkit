package grakkit;

import grakkit.NodeInterop;

import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;

public class Grakkit {

    // The kontext running on the main thread
    public static Kontext kernelKontext; // Changed to base Kontext

    // All kontexts created with the kontext management system
    public static final LinkedList<Kontext> kontexts = new LinkedList<>();

    // All registered class loaders
    public static final HashMap<String, URLClassLoader> classLoaders = new HashMap<>();

    // The Node.js interop context
    private static NodeInterop nodeInterop;

    // Closes all open instances
    public static void close() {
    }

    // Initializes the Grakkit Environment
    public static void init(String root) {
        Paths.get(root).toFile().mkdir();

        // Initialize kernelKontext
        nodeInterop = new NodeInterop("grakkit", root);
        Grakkit.kernelKontext = nodeInterop;

        try {
            Grakkit.kernelKontext.open();
            Grakkit.kernelKontext.execute();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    // Executes the task loop for all kontexts
    public static void tick() {
        if (Grakkit.kernelKontext != null) {
            Grakkit.kernelKontext.tick();
        }
        Grakkit.kontexts.forEach(Kontext::tick);
    }
}