package grakkit.kontexts.api;

import grakkit.Grakkit;
import grakkit.GrakkitConfig;
import grakkit.kontexts.FileKontext;
import grakkit.kontexts.Kontext;
import grakkit.kontexts.ScriptKontext;
import org.graalvm.polyglot.Value;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;

public class KontextAPI {
    // The underlying linked kontext
    public Kontext kontext;

    // Creates a new KontextAPI object around the given kontext
    public KontextAPI(Kontext kontext) {
        this.kontext = kontext;
    }

    // Destroys the current kontext
    public void destroy() throws Exception {
        if (this.kontext == Grakkit.kernelKontext) throw new Exception("The primary instance cannot be destroyed!");
        else this.kontext.destroy();
    }

    // Sends a message into the global event framework. Listeners will fire on next tick
    public void emit(String topic, String payload) {
        this.kontext.messages.add(new KMessage(topic, payload));
    }

    // Creates a new file kontext with the given index path
    public FileKontext fileKontext(String main) {
        return this.fileKontext(main, UUID.randomUUID().toString());
    }

    // Creates a new file instance with the given index path and properties tag
    public FileKontext fileKontext(String main, String props) {
        FileKontext kontext = new FileKontext(props, main, this.kontext.root);
        Grakkit.kontexts.add(kontext);
        return kontext;
    }

    // Gets properties of the current instance
    public String getProperties() {
        return this.kontext.props;
    }

    // Returns the "root" of the current instance
    public String getRoot() {
        return this.kontext.root;
    }

    // Returns the current configuration of Grakkit
    public GrakkitConfig getConfig() {
        return Grakkit.config;
    }

    // Loads the given class from the given source, usually a JAR library
    public Class<?> loadClass(File source, String name) throws ClassNotFoundException, MalformedURLException {
        URL link = source.toURI().toURL();
        String path = source.toPath().normalize().toString();
        return Class.forName(name, true, Grakkit.classLoaders.computeIfAbsent(path, (key) -> new URLClassLoader(
                new URL[] { link },
                Grakkit.class.getClassLoader()
        )));
    }

    // Unsubscribes an event listener from the topic registry
    public boolean off(String topic, Value listener) {
        if (Grakkit.topics.containsKey(topic)) return Grakkit.topics.get(topic).remove(listener);
        else return false;
    }

    // Subscribes an event listener to the topic registry
    public void on(String topic, Value listener) {
        Grakkit.topics.computeIfAbsent(topic, key -> new LinkedList<>()).add(listener);
    }

    // Prepends a script to be executed on the next tick
    public void frontload(Value script) {
        this.kontext.tasks.list.add(script);
    }

    // Creates a new ScriptKontext with the given source code
    public ScriptKontext scriptKontext(String javascript) {
        return this.scriptKontext(javascript, UUID.randomUUID().toString());
    }

    // Creates a new ScriptKontext with the given source code and property tag
    public ScriptKontext scriptKontext(String javascript, String props) {
        ScriptKontext kontext = new ScriptKontext(props, javascript, this.kontext.root);
        Grakkit.kontexts.add(kontext);
        return kontext;
    }

    // Closes and re-opens the current kontext (works best when pushed into the tick loop)
    public void swap() {
        this.kontext.hooks.list.add(Value.asValue((Runnable) () -> this.kontext.open()));
        this.kontext.close();
    }

    // Closes all open kontexts, resets everthing, and swaps the main/kernel kontext
    public void reload() throws Exception {
        if (this.kontext == Grakkit.kernelKontext) {
            new ArrayList<>(Grakkit.kontexts).forEach(Kontext::destroy);
            Grakkit.topics.clear();
            Grakkit.classLoaders.clear();
            this.swap();
        } else throw new Exception("This method may only be called from the main/kernel kontext!");
    }
}