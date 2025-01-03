package grakkit.api;

import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.SourceSection;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSError {

    public static class FrameInfo {

        /**
         * Whether this frame is Java or JS code.
         */
        public final boolean javaFrame;

        /**
         * Source module. For Java frames, this is a fully qualified class name.
         * For JS frames, this is path relative to plugin root.
         */
        public final String source;

        /**
         * Java method or JS function name.
         */
        public final String methodName;

        /**
         * File name (not path).
         */
        public final String fileName;

        /**
         * Line number in the file.
         */
        public final int line;

        public final PolyglotException.StackFrame frame;

        public FrameInfo(PolyglotException.StackFrame frame) {
            this.javaFrame = frame.isHostFrame();

            this.frame = frame;

            if (javaFrame) {
                StackTraceElement hostFrame = frame.toHostFrame();
                this.source = hostFrame.getClassName();
                this.methodName = hostFrame.getMethodName();
                this.fileName = hostFrame.getFileName();
                this.line = hostFrame.getLineNumber();
            } else {
                SourceSection location = frame.getSourceLocation();
                if (location != null) {
                    this.source = location.getSource().getName();
                    this.fileName = Path.of(source).getFileName().toString();
                    this.line = location.getStartLine();
                } else {
                    // Location is not available, fall back to defaults
                    this.source = "unknown";
                    this.fileName = "unknown";
                    this.line = -1;
                }
                this.methodName = frame.getRootName();
            }

        }
    }

    /**
     * Error or exception name.
     */
    public final String name;

    /**
     * Error message.
     */
    public final String message;

    /**
     * Stack frames.
     */
    public final List<FrameInfo> stack;

    public JSError(PolyglotException e) {
        if (e.isHostException()) {
            this.name = e.asHostException().getClass().getName();
            this.message = e.getMessage();
        } else {
            this.name = e.getMessage();
            this.message = null;
        }
        this.stack = new ArrayList<>();
        for (PolyglotException.StackFrame frame : e.getPolyglotStackTrace()) {
            stack.add(new FrameInfo(frame));
        }
    }
}