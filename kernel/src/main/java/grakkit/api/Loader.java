package grakkit.api;

import java.net.URL;
import java.net.URLClassLoader;

public class Loader extends URLClassLoader {
    public Loader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    @Override
    public void addURL(URL location) {
        super.addURL(location);
    }
}