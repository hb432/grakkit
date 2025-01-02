// src/platform/paper/index.ts
export * from './command';
export * from './async';

export const env = {
    name: 'bukkit',
    content: {
        manager: {
            registerEvent: (eventClass: any, listener: any, priority: any, executor: any, plugin: any) => {
                // Simplified implementation for demonstration
                console.log(`Event registered: ${eventClass.name}`);
            },
        },
        plugin: {
            getDataFolder: () => ({
                getParentFile: () => ({
                    getParentFile: () => ({
                        getFreeSpace: () => 1000,
                        getTotalSpace: () => 2000,
                        getUsableSpace: () => 1500,
                    }),
                }),
            }),
        },
        server: {
            getScheduler: () => ({
                runTaskAsynchronously: (_plugin: any, runnable: any) => {
                    runnable.run();
                },
            }),
        },
        Runnable: Java.type('java.lang.Runnable'),
    },
};