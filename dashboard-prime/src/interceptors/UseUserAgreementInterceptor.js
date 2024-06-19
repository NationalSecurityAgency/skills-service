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
import axios from 'axios'
import { useAppInfoState } from '@/stores/UseAppInfoState.js'

export const useUserAgreementInterceptor = () => {

  const appInfoState = useAppInfoState()

  const handleFunction = (response) => {
    const uaHeader = response?.headers?.['skills-display-ua']
    if (uaHeader) {
      appInfoState.setShowUa(true)
    }
  }

  const register = () => {
    axios.interceptors.response.use(
      (response) => {
        handleFunction(response)
        return response
      }
    )
  }

  return {
    register
  }
}