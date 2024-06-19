/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
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