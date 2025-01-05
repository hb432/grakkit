package grakkit;

import grakkit.api.JSLoader;
import grakkit.api.Loader;
import grakkit.interop.NodeInterop;
import grakkit.kontext.FileKontext;
import grakkit.kontext.Kontext;
import org.graalvm.polyglot.Value;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class Grakkit {
    // All registered cross-context channels
    public static final HashMap<String, LinkedList<Value>> topics = new HashMap<>();

    // The kontext running on the main thread
    public static Kontext kernelKontext; // Changed to base Kontext

    // All kontexts created with the kontext management system
    public static final LinkedList<Kontext> kontexts = new LinkedList<>();

    // All registered class loaders
    public static final HashMap<String, URLClassLoader> classLoaders = new HashMap<>();

    // Current Grakkit configuration
    public static GrakkitConfig config;

    // The Node.js interop context
    private static NodeInterop nodeInterop;

    // Closes all open instances
    public static void close() {
        System.out.println("[Grakkit] Closing Grakkit...");
        if (nodeInterop != null) {
            System.out.println("[Grakkit] Closing NodeInterop...");
            nodeInterop.close();
            nodeInterop = null;
            System.out.println("[Grakkit] NodeInterop closed.");
        }
        if (Grakkit.kernelKontext != null) {
            System.out.println("[Grakkit] Closing Kernel Kontext...");
            Grakkit.kernelKontext.close();
            System.out.println("[Grakkit] Kernel Kontext closed.");
        }
        System.out.println("[Grakkit] Destroying Kontexts...");
        new ArrayList<>(Grakkit.kontexts).forEach(kontext -> {
            System.out.println("[Grakkit] Destroying Kontext: " + kontext.props);
            kontext.destroy();
        });
        System.out.println("[Grakkit] Closing GraalVM Engine...");
        Kontext.engine.close();
        System.out.println("[Grakkit] Grakkit closed.");
    }

    // Initializes the Grakkit Environment
    public static void init(String root) {
        Paths.get(root).toFile().mkdir();

        Grakkit.config = new GrakkitConfig(root, "config.yml");

        boolean useNode = Grakkit.config.useNode; // Get the config option

        // Handle node_modules directory
        if (!Paths.get(root, "node_modules", "grakkit").toFile().exists()) {
            try {
                JSLoader.copyJsResources(Grakkit.class, Paths.get(root, "node_modules", "grakkit"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Initialize kernelKontext based on config
        if (useNode) {
            nodeInterop = new NodeInterop("grakkit", root);
            Grakkit.kernelKontext = nodeInterop;
        } else {
            Grakkit.kernelKontext = new FileKontext("grakkit", Grakkit.config.main, root);
        }

        try {
            Grakkit.kernelKontext.open();
            Grakkit.kernelKontext.execute();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    // Locates the given class' true source location
    public static URL locateClassSource(Class<?> clazz) {
        try {
            URL codeSourceLocation = clazz.getProtectionDomain().getCodeSource().getLocation();
            if (codeSourceLocation != null) {
                return codeSourceLocation;
            }
        } catch (SecurityException | NullPointerException ex) {
            // Ignore
        }

        URL classResource = clazz.getResource(clazz.getSimpleName() + ".class");
        if (classResource != null) {
            String resourceLink = classResource.toString();
            String classFileSuffix = clazz.getCanonicalName().replace('.', '/') + ".class";
            if (resourceLink.endsWith(classFileSuffix)) {
                String classPath = resourceLink.substring(0, resourceLink.length() - classFileSuffix.length());
                if (classPath.startsWith("jar:")) {
                    classPath = classPath.substring(4, classPath.length() - 2);
                }
                try {
                    return new URI(classPath).toURL();
                } catch (Throwable ex) {
                    // Ignore
                }
            }
        }
        return null;
    }

    // Executes the task loop for all kontexts
    public static void tick() {
        if (Grakkit.kernelKontext != null) {
            Grakkit.kernelKontext.tick();
        }
        Grakkit.kontexts.forEach(Kontext::tick);
    }

    // Updates the current ClassLoader to one that supports GraalJS
    public static void patch(Loader loader) {
        try {
            loader.addURL(Grakkit.locateClassSource(loader.getClass()));
            Thread.currentThread().setContextClassLoader((ClassLoader) loader);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load classes!", e);
        }
    }
}