package grakkit.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public class JSLoader {
    public static void copyJsResources(Class<?> yourClass, Path targetDir) throws IOException {
        try (InputStream is = yourClass.getResourceAsStream("/js-file-list.txt");
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            String line;
            while ((line = reader.readLine()) != null) {
                try (InputStream jsStream = yourClass.getResourceAsStream("/js/" + line)) {
                    if (jsStream != null) {
                        Path targetPath = targetDir.resolve(line);
                        Files.createDirectories(targetPath.getParent());
                        Files.copy(jsStream, targetPath);
                    } else {
                        System.err.println("Resource not found: " + line);
                    }
                }
            }
        } catch (NullPointerException e) {
            System.err.println("Error: js-file-list.txt not found in resources.");
        }
    }
}