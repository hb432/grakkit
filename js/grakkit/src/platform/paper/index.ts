// src/platform/paper/index.ts
export * from './command';
export * from './async';

const Bukkit = Java.type('org.bukkit.Bukkit') as any;

export const env = {
    name: 'bukkit',
    content: {
        manager: Bukkit.getPluginManager(),
        server: Bukkit.getServer(),
        plugin: Bukkit.getPluginManager().getPlugin('Grakkit'),
        Runnable: Java.type('java.lang.Runnable'),
    },
};

// register js command
export * from './js';