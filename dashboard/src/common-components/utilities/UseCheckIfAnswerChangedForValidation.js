/*
 * Copyright 2025 SkillTree
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
import {useLog} from "@/components/utils/misc/useLog.js";

export const useCheckIfAnswerChangedForValidation = () => {
  const cache = new Map()
  const log = useLog()
  const getStatusIfValueTheSame = (answerId, newValue) => {
    if (!answerId) {
        return null
    }
    const cachedItem = cache.get(answerId)
    if (cachedItem) {
      const isValueSame = cachedItem.value === newValue
      if (log.isTraceEnabled()) {
        log.trace(`useCheckIfAnswerChangedForValidation.hasValueChanged(newValue=${newValue}, answerId=${answerId}): isValueSame=${isValueSame}, cachedItem=${JSON.stringify(cachedItem)}`)
      }
      if (isValueSame) {
        return cachedItem.result
      }
    }
    return null
  }

  const setValueAndStatus = (answerId, value, result) => {
    if (log.isTraceEnabled()) {
      log.trace(`useCheckIfAnswerChangedForValidation.setValueAndStatus(answerId=${answerId}, value=${value}, result=${JSON.stringify(result)})`)
    }
    cache.set(answerId, { value, result })
  }

  const reset = () => {
    log.trace(`useCheckIfAnswerChangedForValidation.reset()`)
    cache.clear()
  }
  return {
    getStatusIfValueTheSame,
    setValueAndStatus,
    reset
  }
}