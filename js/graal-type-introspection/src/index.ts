import ts from 'typescript';
import fg from 'fast-glob';
import path from 'path';
import * as fs from "node:fs";


type JavaTypeKey = string | keyof JavaTypes;
type JavaTypes = { [key: string]: any }; // Simplified
export function processSourceFile(sourceFile: ts.SourceFile) {
    const imports: string[] = [];
    const typeMap: string[] = [];
    function visit(node: ts.Node) {
        if (ts.isModuleDeclaration(node) && node.body) {
            const moduleName = (node as ts.ModuleDeclaration).name.getText().replace(/'|"/g, '');
            ts.forEachChild(node.body, (childNode) => {
                if (ts.isExportDeclaration(childNode) || ts.isClassDeclaration(childNode)) {
                    const isClass = ts.isClassDeclaration(childNode);
                    const exportName = childNode.name!.getText();
                    const uniqueExportName = `${moduleName}.${exportName}`.replace(/\./g, '_');
                    imports.push(`import type { ${exportName} as ${uniqueExportName} } from '${moduleName}'`);
                    typeMap.push(`"${moduleName}.${exportName}": ${isClass ? 'typeof ' : ''}${uniqueExportName}`);
                }
            });
        }
    }
    ts.forEachChild(sourceFile, visit);
    return { imports, typeMap };
}
export function generateTypedefs(fileContent: string) {
    const sourceFile = ts.createSourceFile('temp.ts', fileContent, ts.ScriptTarget.ES2015, true);
    return processSourceFile(sourceFile);
}
export async function findGraalTypeFiles(root: string) {
    const pattern = '@graal-types/**/*.d.ts';
    const files = await fg(pattern, { cwd: path.normalize(root) });
    return files;
}
export async function processGraalTypeFiles(root: string) {
    const files = await findGraalTypeFiles(root);
    const combinedResults: string[] = [];
    const imports = new Set<string>();
    const typeMap = new Set<string>();
    for (const file of files) {
        const fileContent = fs.readFileSync(path.join(root, path.normalize(file)), 'utf8');
        const typedefs = generateTypedefs(fileContent);
        typedefs.imports.forEach((importStatement) => imports.add(importStatement));
        typedefs.typeMap.forEach((typeMapEntry) => typeMap.add(typeMapEntry));
    }
    for (const importStatement of imports) {
        combinedResults.push(importStatement);
    }
    combinedResults.push('');
    combinedResults.push('type JavaTypes = {');
    for (const typeMapEntry of typeMap) {
        combinedResults.push(` ${typeMapEntry},`);
    }
    combinedResults.push('}');
    return combinedResults.join('\n');
}
export function findNodeModules(startPath: string): string | undefined {
    const nodeModules = 'node_modules';
    let currentPath = startPath;
    let count = 0;
    while (path.dirname(currentPath) !== currentPath && count < 10) {
        const potentialPath = path.join(currentPath, nodeModules);
        if (fs.existsSync(potentialPath) && fs.lstatSync(potentialPath).isDirectory()) {
            return potentialPath;
        }
        currentPath = path.dirname(currentPath);
        count++;
    }
    return undefined;
}