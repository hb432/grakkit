// // src/api/context.ts
// export const kontext = {
//     create<X extends 'file' | 'script'>(
//         type: X,
//         content: string,
//         meta?: string
//     ): { file: instance & { main: string }; script: instance & { code: string } }[X] {
//         // @ts-ignore
//         return Grakkit[`${type}Instance`](content, meta);
//     },
//     destroy() {
//         Grakkit.destroy();
//     },
//     emit(topic: string, message: string) {
//         Grakkit.emit(topic, message);
//     },
//     meta: Grakkit.getProperties(),
//     off(topic: string, listener: (data: string) => void) {
//         return Grakkit.off(topic, listener);
//     },
//     on: ((topic: string, listener?: (data: string) => void) => {
//         if (listener) {
//             return Grakkit.on(topic, listener);
//         } else {
//             return new Promise((resolve) => {
//                 const dummy = (response: string) => {
//                     Grakkit.off(topic, dummy);
//                     resolve(response);
//                 };
//                 Grakkit.on(topic, dummy);
//             });
//         }
//     }) as {
//         (topic: string): Promise<string>;
//         (topic: string, listener: (data: string) => void);
//     },
//     swap() {
//         push(Grakkit.swap);
//     },
// };