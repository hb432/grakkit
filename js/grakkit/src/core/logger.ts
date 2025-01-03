import { env } from '../platform/paper';

const logger = env.content.plugin.getLogger();

export function log (...msg: any) {
    logger.log(msg + '');
}
export function warn (...msg: any) {
    logger.warning(msg + '');
}
export function error (...msg: any) {
    logger.severe(msg + '');
}