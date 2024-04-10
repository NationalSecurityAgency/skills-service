<script setup>
import { ref } from 'vue'
import { tryOnBeforeMount, useDebounceFn } from '@vueuse/core'
import Postmate from 'postmate'

import { useSkillsDisplayParentFrameState } from './stores/UseSkillsDisplayParentFrameState'
import { useLog } from '@/components/utils/misc/useLog.js'
import SkillsDisplay from '@/skills-display/components/SkillsDisplay.vue'
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import ThemeHelper from '@/skills-display/theme/ThemeHelper.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'

const parentState = useSkillsDisplayParentFrameState()
const displayPreferences = useSkillsDisplayPreferencesState()
const displayAttributes = useSkillsDisplayAttributesState()
const log = useLog()

const getDocumentHeight = () => {
  const { body } = document
  return Math.max(body.scrollHeight, body.offsetHeight)
}

const onHeightChanged = useDebounceFn(() => {
  // if (process.env.NODE_ENV !== 'development') {
  parentState.parentFrame.emit('height-changed', getDocumentHeight())
  // }
}, 0)

tryOnBeforeMount(() => {

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
    window.addEventListener('resize', onHeightChanged)
    onHeightChanged()

    // will only display summary and component will not be interactive
    displayPreferences.isSummaryOnly = parent.model.isSummaryOnly ? parent.model.isSummaryOnly : false

    // whether or not to use an internal back button as opposed to the browser back button
    // displayPreferences.internalBackButton = parent.model.internalBackButton == null || parent.model.internalBackButton

    displayAttributes.projectId = parent.model.projectId
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
      appStyleObject.value['min-height'] = parent.model.minHeight
    }

    // // No scroll bars for iframe.
    // document.body.style['overflow-y'] = 'hidden';
    //
    // this.loadConfigs();
    // this.getCustomIconCss();
  })
})

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

</script>

<template>
  <div
    id="skills-display-app"
    ref="content"
    tabindex="-1"
    role="main"
    :style="appStyleObject"
    aria-label="SkillTree Skills Display">
<!--    <skills-spinner :is-loading="true" />-->
    <skills-display />
  </div>
</template>

<style scoped>

</style>