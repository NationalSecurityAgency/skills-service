import log from 'loglevel';

export const useLog = () => {
  const trace = (message) => {
    log.trace(message)
  }
  const isTraceEnabled = () => {
    return log.getLevel() <= log.levels.TRACE
  }
  return {
    trace,
    isTraceEnabled
  }
}