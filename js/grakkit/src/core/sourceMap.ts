// src/core/sourceMap.ts
import * as vlq from 'vlq';
import { SourceLine, SourceMap } from '../type/types';

function createCachedMap(raw: any): SourceMap {
    const lines = raw.mappings.split(';').map((line: string) => line.split(','));
    const decoded = lines.map((line: string[]) => line.map((col: string) => vlq.decode(col)));
    return { sources: raw.sources, mappings: decoded, startOffset: 0 };
}

function loadSourceMap(fileContents: string): SourceMap | undefined {
    try {
        const sourceMap = JSON.parse(fileContents);
        return createCachedMap(sourceMap);
    } catch (e) {
        return undefined;
    }
}

const cachedMaps: Map<string, SourceMap> = new Map();

export function cacheSourceMap(file: string, content: string, startOffset: number): boolean {
    const map = loadSourceMap(content);
    if (map) {
        map.startOffset = startOffset;
        cachedMaps.set(file, map);
        return true;
    }
    return false;
}

function mapLineInternal({ mappings, sources }: SourceMap, jsLine: number): SourceLine {
    let sourceLine = 0;
    let sourceFile = 0;
    let result = 0;
    for (let i = 0; i < mappings.length; i++) {
        const line = mappings[i];
        line.forEach((segment) => {
            sourceLine += segment[1] ?? 0;
            sourceFile += segment[2] ?? 0;
        });
        if (i + 1 === jsLine) {
            result = sourceLine + 1;
            return { file: sources[sourceFile], line: result };
        }
    }
    throw new Error(`source map failed for line ${jsLine}`);
}

export function mapLineToSource(file: string, line: number): SourceLine {
    const map = cachedMaps.get(`${file}`);
    if (map) {
        line -= map.startOffset;
        if (line <= 0) {
            return { file: file, line: line };
        }
        const result = mapLineInternal(map, line);
        if (result.file.startsWith('webpack://test/')) {
            result.file = result.file.replace('webpack://test/', '');
        }
        if (result.file.startsWith('../')) {
            result.file = result.file.replace('../', './');
        }
        return result;
    } else {
        return { file, line };
    }
}