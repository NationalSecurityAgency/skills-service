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

import CustomIconService from '@/components/icons/CustomIconService.js'

const createCustomIconStyleElementIfNotExist = (projectId) => {
  const head = document.getElementsByTagName('head')[0]

  const lookupId = `skill-custom-icons${projectId ? `-${projectId}` : ''}`
  let existingStyles = document.getElementById(lookupId)
  if (!existingStyles) {
    existingStyles = document.createElement('style')
    existingStyles.id = 'skill-custom-icons'
    existingStyles.type = 'text/css'
    head.appendChild(existingStyles)
  }

  return existingStyles
}

export default {
  getIconIndex(projectId) {
    let url = `/app/projects/${encodeURIComponent(projectId)}/customIcons`
    if (!projectId) {
      url = '/supervisor/icons/customIcons'
    }
    return axios.get(url).then((response) => response.data)
  },
  deleteIcon(iconName, projectId) {
    let url = `/admin/projects/${encodeURIComponent(projectId)}/icons/${iconName}`
    if (!projectId) {
      url = `/supervisor/icons/${iconName}`
    }
    return axios.delete(url)
  },
  addCustomIconCSS(css) {
    const existingStyles = createCustomIconStyleElementIfNotExist()
    existingStyles.innerText += css
  },
  refreshCustomIconCss(projectId, isSupervisor) {
    const existingStyles = createCustomIconStyleElementIfNotExist(projectId)
    CustomIconService.getCustomIconCss(projectId, isSupervisor).then((response) => {
      if (response) {
        existingStyles.innerText = response
      }
    })
  },
  findUsages(projectId, iconClass) {
    let url = `/admin/projects/${encodeURIComponent(projectId)}/icons/${iconClass}/usage`
    return axios.get(url).then((response) => response.data)
  }
}
