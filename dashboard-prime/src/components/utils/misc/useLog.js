import log from 'loglevel';

export const useLog = () => {
  const trace = (message) => {
    log.trace(message)
  }
  const debug = (message) => {
    log.debug(message)
  }
  const info = (message) => {
    log.info(message)
  }
  const warn = (message) => {
    log.warn(message)
  }
  const error = (message) => {
    log.error(message)
  }
  const isTraceEnabled = () => {
    return log.getLevel() <= log.levels.TRACE
  }
  const isDebugEnabled = () => {
    return log.getLevel() <= log.levels.DEBUG
  }
  return {
    trace,
    isTraceEnabled,
    debug,
    isDebugEnabled,
    info,
    warn,
    error
  }
}