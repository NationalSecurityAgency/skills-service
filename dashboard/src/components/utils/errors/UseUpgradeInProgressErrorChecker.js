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
import { useRouter } from 'vue-router'

export const useUpgradeInProgressErrorChecker = () => {

  const router = useRouter()

  const isUpgrading = (error) => error?.response?.status === 503 && error?.response?.data?.errorCode === 'DbUpgradeInProgress'
  const navToUpgradeInProgressPage = () => {
    router.push({ name: 'DbUpgradeInProgressPage' });
  }
  const checkError = (error) => {
    if (isUpgrading(error)) {
      navToUpgradeInProgressPage()
      return true
    }
    return false
  }
  return {
    isUpgrading,
    navToUpgradeInProgressPage,
    checkError,
  }
}