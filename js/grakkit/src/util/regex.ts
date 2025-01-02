// src/utils/regex.ts
export const regex = {
    replace: (input: string, pattern: string, replacement: string) => {
        return input.replace(new RegExp(pattern), replacement);
    },
    test: (input: string, pattern: string) => {
        return new RegExp(pattern).test(input);
    }
};