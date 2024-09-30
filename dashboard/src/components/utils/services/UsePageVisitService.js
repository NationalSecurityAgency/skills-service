/*
 * Copyright 2020 SkillTree
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
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'


export const usePageVisitService = () => {

  const appConfig = useAppConfig()
  const reportPageVisit = (path, fullPath) => {
     if (appConfig.enablePageVisitReporting) {
      const domain = new URL(window.location)
      axios.put(
        '/api/pageVisit',
        {
          path,
          fullPath,
          hostname: domain.hostname,
          port: domain.port,
          protocol: domain.protocol,
          skillDisplay: false
        },
        { handleError: false }
      )
    }
  }

  return {
    reportPageVisit
  }
}
