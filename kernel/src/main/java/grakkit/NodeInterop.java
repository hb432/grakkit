package grakkit;

import com.caoccao.javet.enums.JSRuntimeType;
import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interception.jvm.JavetJVMInterceptor;
import com.caoccao.javet.interop.NodeRuntime;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.converters.JavetProxyConverter;
import com.caoccao.javet.interop.engine.IJavetEngine;
import com.caoccao.javet.interop.engine.JavetEnginePool;
import com.caoccao.javet.node.modules.NodeModuleModule;
import com.caoccao.javet.utils.JavetOSUtils;
import com.caoccao.javet.values.reference.V8ValueObject;
import grakkit.Kontext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NodeInterop extends Kontext {

    private static class Linkage {
        LinkedBlockingDeque<Runnable> taskQueue;
        Thread nodeJSThread;
        NodeRuntime nodeRuntime;
        CountDownLatch initLatch;
        JavetJVMInterceptor javetJVMInterceptor;

        Linkage(LinkedBlockingDeque<Runnable> taskQueue, Thread nodeJSThread, NodeRuntime nodeRuntime, CountDownLatch initLatch, JavetJVMInterceptor javetJVMInterceptor) {
            this.taskQueue = taskQueue;
            this.nodeJSThread = nodeJSThread;
            this.nodeRuntime = nodeRuntime;
            this.initLatch = initLatch;
            this.javetJVMInterceptor = javetJVMInterceptor;
        }
    }

    private volatile static Linkage linkage;
    private final AtomicBoolean booted = new AtomicBoolean(false);
    private Thread nodeThread;
    private JavetEnginePool<NodeRuntime> javetEnginePool;

    public NodeInterop(String props, String root) {
        super(props, root);
    }

    public Class<? extends NodeInterop> thisClass() {
        return this.getClass();
    }

    @Override
    public void open() {
        this.ignoreMultithreadError = true;
        this.thisID = Kontext.nextID++;
        this.tickCount = 0;

        // Initialize Javet Engine Pool for NodeRuntime
        javetEnginePool = new JavetEnginePool<>();
        javetEnginePool.getConfig().setJSRuntimeType(JSRuntimeType.Node);
        javetEnginePool.getConfig().setGlobalName("globalThis");

        // Initialize NodeJS environment in a separate thread
        LinkedBlockingDeque<Runnable> taskQueue = new LinkedBlockingDeque<>();
        CountDownLatch initLatch = new CountDownLatch(1); // Initialize latch
        nodeThread = new Thread(() -> {
            IJavetEngine<NodeRuntime> engine = null;
            try {
                // Get a Javet engine from the pool.
                engine = javetEnginePool.getEngine();
                NodeRuntime nodeRuntime = engine.getV8Runtime();
                System.out.println("NodeJS Bootstrap Thread: Got NodeRuntime");

                // Set the require root directory so that Node.js is able to locate node_modules.
                File workingDirectory = new File(JavetOSUtils.WORKING_DIRECTORY + "/plugins/grakkit");
                nodeRuntime.getNodeModule(NodeModuleModule.class).setRequireRootDirectory(workingDirectory);
                // log the full path of workingDirectory
                System.out.println("NodeJS Bootstrap Thread: Working Directory: " + workingDirectory.getAbsolutePath());

                // Create and register the Javet JVM interceptor.
                JavetJVMInterceptor interceptor = new JavetJVMInterceptor(nodeRuntime);
                interceptor.register(nodeRuntime.getGlobalObject());

                linkage = new Linkage(taskQueue, Thread.currentThread(), nodeRuntime, initLatch, interceptor);
                System.out.println("NodeJS Bootstrap Thread: Linkage created");

                JavetProxyConverter javetProxyConverter = new JavetProxyConverter();
                nodeRuntime.setConverter(javetProxyConverter);

                // inject grakkit.interop.NodeInterop
                nodeRuntime.getGlobalObject().set("NodeInterop", thisClass());

                // Execute the boot.js script
                //language=JavaScript
                String bootCode = """
                    let java = javet.package.java;
                    let javaToJSQueue = new java.util.concurrent.LinkedBlockingDeque();
                    const { Worker, isMainThread, parentPort, workerData } = require('worker_threads');
                    let worker = new Worker(`
                      const { workerData, parentPort } = require('worker_threads');
                      let data = null;
                      function tick () {
                        if (data == workerData) tick;
                        data = workerData;
                        parentPort.postMessage(data);
                      }
                    `, { eval: true, workerData: javaToJSQueue });

                    worker.on('message', (callback) => {
                      try {
                          typeof callback === 'function' && callback();
                      } catch (e) {
                        console.error(e);
                      }
                    });
                    NodeInterop.boot(javaToJSQueue, process.env.ARGS ? process.env.ARGS.split(' ') : []);
                    """;

                nodeRuntime.getGlobalObject().set("process", nodeRuntime.createV8ValueObject());
                V8ValueObject process = nodeRuntime.getGlobalObject().get("process");
                process.set("env", nodeRuntime.createV8ValueObject());
                V8ValueObject env = process.get("env");
                env.set("ARGS", "");

                System.out.println("NodeJS Bootstrap Thread: Executing boot script");
                nodeRuntime.getExecutor(bootCode).executeVoid();
                System.out.println("NodeJS Bootstrap Thread: Boot script executed");
                initLatch.countDown(); // Signal that initialization is complete

                System.out.println("NodeJS Bootstrap Thread: initLatch counted down");

                // Process tasks from the queue
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Runnable task = taskQueue.poll(1, TimeUnit.SECONDS); // Poll with a timeout
                        if (task != null) {
                            task.run();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                System.out.println("NodeJS Bootstrap Thread: Task processing loop finished.");

            } catch (Throwable e) {
                System.out.println("NodeJS Bootstrap Thread: Hello from the NodeJS Bootstrap Thread. we messed up");
                e.printStackTrace();
            } finally {
                if (engine != null) {
                    try {
                        javetEnginePool.releaseEngine(engine);
                        System.out.println("NodeJS Bootstrap Thread: Engine released.");
                    } catch (Throwable e) {
                        System.err.println("Error releasing Javet engine: " + e.getMessage());
                    }
                }
            }
        }, "NodeJS Bootstrap Thread");
        nodeThread.start();
    }

    @SuppressWarnings("unused") // Called from boot.js
    public static void boot(LinkedBlockingDeque<Runnable> taskQueue, String[] args) {
        if (linkage != null) {
            if (args.length > 0) {
                System.out.println("Arguments passed to NodeJS: " + Arrays.toString(args));
                // Handle arguments as needed
            }
        }
    }
    @Override
    public void close () {
        V8Host host = V8Host.getInstance(JSRuntimeType.Node);
        V8Host.setLibraryReloadable(true);
        host.unloadLibrary();
        Thread closethread = new Thread(() -> {

            try {
//                linkage.nodeRuntime.getExecutor("process.emit('close')").executeVoid();
                linkage.nodeRuntime.lowMemoryNotification();
//                linkage.nodeRuntime.setPurgeEventLoopBeforeClose(true);
                linkage.nodeRuntime.lowMemoryNotification();
                linkage.nodeRuntime.terminateExecution();
                linkage.nodeRuntime.lowMemoryNotification();
                linkage.nodeRuntime.close(true);
            } catch (JavetException e) {
                e.printStackTrace();
            }


        });
        closethread.start();
    }

    @Override
    public void destroy() {
        System.out.println("NodeInterop destroy() called.");
        close();
        super.destroy();
        System.out.println("NodeInterop destroyed.");
    }

    @Override
    public void execute() throws Throwable {
        // Wait for initialization to complete
        if (linkage == null) {
            synchronized (this) {
                while (linkage == null) {
                    try {
                        wait(100); // Wait for a short period
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("NodeJS environment initialization interrupted.", e);
                    }
                }
            }
        }
        linkage.initLatch.await();
        if (linkage == null) {
            throw new IllegalStateException("NodeJS environment not initialized (linkage is null after await).");
        }

        File index = Paths.get(this.root).resolve("index.js").toFile();
        // Create the file if it doesn't exist
        index.createNewFile();
        // Add default content to the new file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(index))) {
            writer.write("console.log('JavaScript for Minecraft, with the Graal Kernel Kit.');\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Execute the main script
        try {
            runJS(() -> {
                try {
                    linkage.nodeRuntime.getExecutor(index).executeVoid();
                } catch (JavetException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute main script after creation: ");
        }
    }

    public static boolean isOnMainNodeThread() {
        return linkage != null && Thread.currentThread() == linkage.nodeJSThread;
    }

    public static void checkOnMainNodeThread() {
        if (!isOnMainNodeThread()) {
            throw new IllegalStateException("You are not currently on the NodeJS thread.");
        }
    }

    public static <T> T runJS(SupplierWithException<T> supplier) throws Throwable {
        if (linkage == null) {
            throw new IllegalStateException("NodeJS environment not initialized in Supplier.");
        }
        if (isOnMainNodeThread()) {
            return supplier.get();
        } else {
            java.util.concurrent.CompletableFuture<T> future = new java.util.concurrent.CompletableFuture<>();
            linkage.taskQueue.add(() -> {
                try {
                    future.complete(supplier.get());
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            });
            return future.get();
        }
    }

    public static void runJS(RunnableWithException runnable) throws Throwable {
        if (linkage == null) {
            throw new IllegalStateException("NodeJS environment not initialized in Runnable.");
        }
        if (isOnMainNodeThread()) {
            runnable.run();
        } else {
            java.util.concurrent.CompletableFuture<Void> future = new java.util.concurrent.CompletableFuture<>();
            linkage.taskQueue.add(() -> {
                try {
                    runnable.run();
                    future.complete(null);
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            });
            future.get();
        }
    }

    @FunctionalInterface
    public interface SupplierWithException<T> {
        T get() throws Throwable;
    }

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Throwable;
    }
}