package grakkit.api;

import org.graalvm.polyglot.PolyglotException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSError {

    /** The exception which caused the error. */
    public final PolyglotException error;

    /** The message associated with this error. */
    public final String message;

    /** The full stack trace associated with this error. */
    public final String stack;

    /** The formatted stack trace associated with this error. */
    public final String trace;

    /**
     * Constructs a new error with the given exception.
     *
     * @param error the exception
     */
    public JSError(PolyglotException error) {
        this.error = error;
        this.message = error.getMessage();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        error.printStackTrace(new PrintStream(output));
        String[] lines = output.toString().replace("\r", "").split("\n");
        StringBuilder builder = new StringBuilder();
        for (int index = 1; index < lines.length; index++) {
            String line = lines[index].replace("\t", "  ");
            Matcher matcher = Pattern.compile("^\s*at ([^:]+):([0-9]+)$").matcher(line);
            if (matcher.matches()) {
                builder.append(matcher.group(1) + " @ " + matcher.group(2) + "\n");
            }
        }
        this.stack = builder.toString();
        Matcher matcher = Pattern.compile("^(\\S+): (.+)$").matcher(this.message);
        if (matcher.matches()) {
            this.trace = matcher.group(2) + "\n\n" + this.stack;
        } else {
            this.trace = this.message + "\n\n" + this.stack;
        }
    }
}