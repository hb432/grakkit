package grakkit.interop;

import io.reactivex.Completable;
import org.orienteer.jnpm.JNPMService;
import org.orienteer.jnpm.JNPMSettings;

import java.nio.file.Paths;

public class NPMInterop {
    public JNPMService service;
    public NPMInterop(String root) {
        // init npm service
        JNPMService.configure(JNPMSettings.builder().homeDirectory(Paths.get(root, "npm")).build());
        service = JNPMService.instance();
    }
    // method to download a Grakkit package from npm
    public void downloadPackage(String packageName) {
        JNPMService service = JNPMService.instance();
        Completable pack = service.bestMatch("@grakkit/stdlib", "latest").downloadTarball();
    }

}
