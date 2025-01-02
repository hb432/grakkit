// src/api/config.ts
import { GrakkitConfig } from '../type/types';

export const config: GrakkitConfig = {
    main: 'index.js',
    initialize: true,
    verbose: false,
    pluginName: 'Grakkit',
};

export const setConfig = (userConfig: Partial<GrakkitConfig>) => {
    Object.assign(config, userConfig);
};