package grakkit.interop;

import org.orienteer.jnpm.InstallationStrategy;
import org.orienteer.jnpm.RxJNPMService;
import org.orienteer.jnpm.traversal.ITraversalRule;
import org.orienteer.jnpm.traversal.TraverseDirection;

import java.nio.file.Path;
import java.nio.file.Paths;

// the Pack class, capable of downloading and managing Grakkit packages from npm
// the Pack represents a Grakkit npm package, and contains methods to...
// disable, enable
// download, install, remove
// list info about, version, updates behind/outdated, etc.
// update
// check for updates
// note, this is all for single packages, not for the entire project!
public class Pack {
    String packageName;
    Path root;
    RxJNPMService service;
    // the pack is generated based on whether the package is valid
    // the data is mostly retrieved from jNPM
    // main:
    public Pack (String packageName, String root) {
        // check if the package is valid
        // if it is, generate the pack
        if (isValid(packageName)) {
            // generate the pack
            this.packageName = packageName;
            this.service = new NPMInterop(root).service.getRxService();
            this.root = Paths.get(root, "npm");
        }
    }
    // download the package
    public void download() {
        // download the package
        service.traverse(TraverseDirection.WIDER, ITraversalRule.DEPENDENCIES, packageName).subscribe(t -> {
            t.install(root, InstallationStrategy.NPM).blockingAwait();
        });

    }
    private boolean isValid(String name) {
        return name.startsWith("@grakkit/");
    }
}
