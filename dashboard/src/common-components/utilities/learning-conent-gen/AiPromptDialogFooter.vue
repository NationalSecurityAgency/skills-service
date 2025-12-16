/*
Copyright 2025 SkillTree

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
import {computed} from "vue";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";

const appConf = useAppConfig()

const hasPoweredByInfo = computed(() => appConf.openaiFooterPoweredByLink && appConf.openaiFooterPoweredByLinkText)
const hasInfo = computed(() => appConf.openaiFooterMsg || hasPoweredByInfo.value)
</script>

<template>
  <div v-if="hasInfo"
       class="text-sm text-gray-500 flex flex-col sm:flex-row gap-2 text-left flex-1 pt-3"
       data-cy="aiPromptDialogFooter">
    <div v-if="appConf.openaiFooterMsg"
         data-cy="aiPromptDialogFooterMsg"
         class="flex-1 text-left">
      <i class="fa-solid fa-spell-check"></i> {{ appConf.openaiFooterMsg }}
    </div>
    <div v-if="hasPoweredByInfo"
         data-cy="aiPromptDialogFooterPoweredBy">
      <i class="fa-solid fa-plug-circle-check" aria-hidden="true"></i> Powered By <a :href="appConf.openaiFooterPoweredByLink" class="underline" target="_blank">{{ appConf.openaiFooterPoweredByLinkText }} <i class="fa-solid fa-external-link" aria-hidden="true"></i></a>
    </div>
  </div>
</template>

<style scoped>

</style>