import { useSkillsDisplayParentFrameState } from '@/skills-display/stores/UseSkillsDisplayParentFrameState.js'
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useLog } from '@/components/utils/misc/useLog.js'
import Postmate from 'postmate'
import { ref } from 'vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import ThemeHelper from '@/skills-display/theme/ThemeHelper.js'

export const useIframeInit = () => {

  const parentState = useSkillsDisplayParentFrameState()
  const displayPreferences = useSkillsDisplayPreferencesState()
  const displayAttributes = useSkillsDisplayAttributesState()
  const log = useLog()
  const loadedIframe = ref(false)

  const handleHandshake =() => {
    log.debug('SkillsDisplayInIframe.vue: tryOnBeforeMount')
    const handshake = new Postmate.Model({
      updateAuthenticationToken(authToken) {
        parentState.authToken = authToken
        if (log.isTraceEnabled()) {
          log.trace(`SkillsDisplay.vue: updateAuthenticationToken: ${authToken}`)
        }
      }
    })
    handshake.then((parent) => {
      log.debug('SkillsDisplayInIframe.vue: handshake.then')
      // Make sure to freeze the parent object so Pinia won't try to make it reactive
      // CORs won't allow this because parent object can't be changed from an iframe
      parentState.parentFrame = Object.freeze(parent)
      const resizeObserver = new ResizeObserver(function(entries) {
        const observedEntry = entries[0].contentRect;
        const newHeight = observedEntry.height + 10;
        log.debug(`SkillsDisplayInIframe.vue: changing height to [${newHeight}]`)
        parentState.parentFrame.emit('height-changed', newHeight)
      });
      resizeObserver.observe(document.querySelector("body"))

      // will only display summary and component will not be interactive
      displayPreferences.isSummaryOnly = parent.model.isSummaryOnly ? parent.model.isSummaryOnly : false

      // whether to use an internal back button as opposed to the browser back button
      // displayPreferences.internalBackButton = parent.model.internalBackButton == null || parent.model.internalBackButton

      displayAttributes.projectId = parent.model.projectId
      displayAttributes.serviceUrl = parent.model.serviceUrl
      log.debug(`SkillsDisplayInIframe.vue: serviceUrl: [${displayAttributes.serviceUrl}], projectId: [${displayAttributes.projectId}]`)
      parentState.serviceUrl = parent.model.serviceUrl

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
        log.debug(`SkillsDisplayInIframe.vue: parent.model.minHeight: ${parent.model.minHeight}`)
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

      style.id = themeState.theme.themeStyleId;
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