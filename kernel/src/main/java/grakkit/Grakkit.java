package grakkit;

import grakkit.api.JSLoader;
import grakkit.api.Loader;
import grakkit.kontexts.FileKontext;
import grakkit.kontexts.Kontext;
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
    public static FileKontext kernelKontext;

    // All kontexts created with the kontext management system
    public static final LinkedList<Kontext> kontexts = new LinkedList<>();

    // All registered class loaders
    public static final HashMap<String, URLClassLoader> classLoaders = new HashMap<>();

    // Current Grakkit configuration
    public static GrakkitConfig config;

    // Closes all open instances
    public static void close() {
        Grakkit.kernelKontext.close();
        new ArrayList<>(Grakkit.kontexts).forEach(Kontext::destroy);
        Kontext.engine.close();
    }

    // Initializes the Grakkit Environment
    public static void init(String root) {
        Paths.get(root).toFile().mkdir();

        Grakkit.config = new GrakkitConfig(root, "config.yml");
        // first verify that node_modules/grakkit exists, and if not, create it and copy the js resources
        if (!Paths.get(root, "node_modules", "grakkit").toFile().exists()) {
            try {
                JSLoader.copyJsResources(Grakkit.class, Paths.get(root, "node_modules", "grakkit"));
                Grakkit.kernelKontext = new FileKontext("grakkit", Grakkit.config.main, root);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // add the lines console.log('Modern JS for Minecraft, with the Graal Kernel Kit!') and require('grakkit/index.js') to the main file
        // if they don't already exist
        Grakkit.kernelKontext = new FileKontext("grakkit", Grakkit.config.main, root);
        try {
            Grakkit.kernelKontext.open();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    // Locates the given class' true source location
    /**
     * Locates the base URL for the given class. This method tries to
     * find the source location (JAR or directory) where the class is loaded from.
     * @param clazz The class whose location should be resolved.
     * @return The URL of the class's source location, or null if not found.
     */
    public static URL locateClassSource(Class<?> clazz) {

        // 1. Attempt to get the class location from the ProtectionDomain

        try {
            URL codeSourceLocation = clazz.getProtectionDomain().getCodeSource().getLocation();
            if (codeSourceLocation != null) {
                return codeSourceLocation; // Return if we get the location from ProtectionDomain.
            }
        } catch (SecurityException | NullPointerException ex) {
            // Unable to get location from the ProtectionDomain.
            // Proceed to try the class resource.
        }

        // 2. Attempt to get the class location using getResource

        URL classResource = clazz.getResource(clazz.getSimpleName() + ".class");

        if (classResource != null) {
            String resourceLink = classResource.toString();
            String classFileSuffix = clazz.getCanonicalName().replace('.', '/') + ".class";

            if (resourceLink.endsWith(classFileSuffix)) {
                // extract path from the url using the class suffix
                String classPath = resourceLink.substring(0, resourceLink.length() - classFileSuffix.length());
                if (classPath.startsWith("jar:")) {
                    // trim `jar:` prefix and `!/` at the end
                    classPath = classPath.substring(4, classPath.length() - 2);
                }

                try {
                    // construct path and return it.
                    return new URI(classPath).toURL();
                } catch (Throwable ex) {
                    // fail when path construction fails, simply return null.
                }
            }
        }

        // No valid source can be found
        return null;
    }

    // Executes the task loop for all kontexts
    public static void tick() {
        Grakkit.kernelKontext.tick();
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