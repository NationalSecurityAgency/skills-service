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
import { useSkillsDisplayParentFrameState } from '@/skills-display/stores/UseSkillsDisplayParentFrameState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useLog } from '@/components/utils/misc/useLog.js'
import Postmate from 'postmate'
import { ref } from 'vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import ThemeHelper from '@/skills-display/theme/ThemeHelper.js'

export const useIframeInit = () => {

  const parentState = useSkillsDisplayParentFrameState()
  const displayAttributes = useSkillsDisplayAttributesState()
  const log = useLog()
  const loadedIframe = ref(false)

  const registerHeightListener = (resizeObserver) => {
    const elementToObserve = document.querySelector("#skills-display-app")
    if (elementToObserve) {
      resizeObserver.observe(elementToObserve)
    } else {
      setTimeout(() => {
        registerHeightListener(resizeObserver)
      }, 100);
    }
  }

  const handleHandshake =() => {
    log.debug('UseIframeInit.js: handleHandshake')
    const handshake = new Postmate.Model({
      updateAuthenticationToken(authToken) {
        parentState.authToken = authToken
        if (log.isTraceEnabled()) {
          log.trace(`UseIframeInit.js: updateAuthenticationToken: ${authToken}`)
        }
      }
    })
    handshake.then((parent) => {
      log.debug('UseIframeInit.js: handshake.then')
      // Make sure to freeze the parent object so Pinia won't try to make it reactive
      // CORs won't allow this because parent object can't be changed from an iframe
      parentState.parentFrame = Object.freeze(parent)
      const resizeObserver = new ResizeObserver(function(entries) {
        const observedEntry = entries[0].contentRect;
        if (observedEntry && observedEntry.height > 0) {
          const newHeight = observedEntry.height + 10;
          log.debug(`UseIframeInit.js: changing height to [${newHeight}]`)
          parentState.parentFrame.emit('height-changed', newHeight)
        } else {
          log.warn('UseIframeInit.js: resize event got height of 0')
        }
      });
      registerHeightListener(resizeObserver)

      // will only display summary and component will not be interactive
      displayAttributes.isSummaryOnly = parent.model.isSummaryOnly ? parent.model.isSummaryOnly : false

      // whether to use an internal back button as opposed to the browser back button
      // displayAttributes.internalBackButton = parent.model.internalBackButton == null || parent.model.internalBackButton

      parentState.serviceUrl = parent.model.serviceUrl

      displayAttributes.projectId = parent.model.projectId
      displayAttributes.serviceUrl = parent.model.serviceUrl
      log.debug(`UseIframeInit.js: serviceUrl: [${displayAttributes.serviceUrl}], projectId: [${displayAttributes.projectId}]`)

      if (parent.model.options) {
        parentState.options = { ...parent.model.options }
      }

      // UserSkillsService.setVersion(parent.model.version);
      // UserSkillsService.setUserId(parent.model.userId);
      // QuizRunService.setUserId(parent.model.userId);
      //
      handleTheming(parent.model.theme);

      parentState.parentFrame.emit('needs-authentication')

      if (parent.model.minHeight) {
        log.debug(`UseIframeInit.js: parent.model.minHeight: ${parent.model.minHeight}`)
        appStyleObject.value['min-height'] = parent.model.minHeight
      }

      // No scroll bars for iframe.
      document.body.style['overflow-y'] = 'hidden';

      // this.loadConfigs();
      displayAttributes.loadingConfig = false
      // this.getCustomIconCss();

      loadedIframe.value = true
    })
  }

  const appStyleObject = ref({})
  const themeState = useSkillsDisplayThemeState()
  const handleTheming = (theme) =>{
    if (theme) {
      const themeResArtifacts = ThemeHelper.build(theme);

      // populate store so JS can subscribe to those values and update styles
      themeResArtifacts.themeModule.forEach((value, key) => {
        themeState.setThemeByKey(key, value)
      });

      const style = document.createElement('style');

      style.id = themeState.themeStyleId;
      style.appendChild(document.createTextNode(themeResArtifacts.css));

      const { body } = document;
      body.appendChild(style);
    }
  }

  return {
    handleHandshake,
    loadedIframe
  }
}