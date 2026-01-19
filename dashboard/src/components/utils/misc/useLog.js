/*
 * Copyright 2024 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import log from 'loglevel';
import dayjs from '@/common-components/DayJsCustomizer'

export const useLog = () => {
  const trace = (message) => {
    log.trace(message)
  }
  const debug = (message) => {
    if (isDebugEnabled()) {
      const timestamp = dayjs().format('HH:mm:ss')
      log.debug(`[${timestamp}] ${message}`);
    }
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