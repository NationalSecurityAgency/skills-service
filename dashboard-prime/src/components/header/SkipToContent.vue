<script setup>
import { nextTick } from 'vue'
import { useLog } from '@/components/utils/misc/useLog.js'

const log = useLog()

const focusOnMainContent = () => {
  const toSearch = ['mainContent3', 'mainContent2', 'mainContent1'];
  //  there are currently only 3 levels
  nextTick(() => {
    const foundId = toSearch.find((id) => document.getElementById(id));
    log.debug(`Found id ${foundId} after checking ${toSearch}`)
    if (foundId) {
      nextTick(() => {
        const focusOn = document.getElementById(foundId);
        if (focusOn) {
          focusOn.focus({});
        }
      });
    }
  });
}
</script>

<template>
<div>
  <!--  see usage of preSkipToContentPlaceholder in main.js  -->
  <span id="preSkipToContentPlaceholder" tabindex="-1" aria-hidden="true" data-cy="preSkipToContentPlaceholder"/>
  <SkillsButton
    class="skip-main"
    @click="focusOnMainContent"
    @keydown.prevent.enter="focusOnMainContent"
    tabindex="0"
    :outlined="false"
    data-cy="skipToContentButton">Skip to content</SkillsButton>
</div>
</template>

<style scoped>
.skip-main {
  position: absolute !important;
  overflow: hidden !important;
  z-index: -9999 !important;
  opacity: 0 !important;
}

.skip-main:focus, .skip-main:active {
  visibility: visible !important;
  opacity: 100 !important;
  left: 5px !important;
  top: 5px !important;
  font-size: 1.2em !important;
  z-index: 999 !important;
}
</style>